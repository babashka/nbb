(ns nbb.impl.nrepl
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["fs" :as fs]
   ["net" :as node-net]
   ["path" :as path]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.core :as nbb]
   [nbb.impl.bencode :refer [encode decode-all]]
   [sci.core :as sci])
  (:require-macros
   [nbb.macros :refer [with-async-bindings]]))

(defn debug [& strs]
  (.debug js/console (str/join " " strs)))

(defn warn [& strs]
  (.warn js/console (str/join " " strs)))

(defn info [& strs]
  (.info js/console (str/join " " strs)))

(defn response-for-mw [handler]
  (fn [{:keys [id session] :as request} response]
    (let [response (cond-> (assoc response
                                  "id" id)
                     session (assoc "session" session))]
      (handler request response))))

(defn coerce-request-mw [handler]
  (fn [request send-fn]
    (handler (update request :op keyword) send-fn)))

(defn log-request-mw [handler]
  (fn [request send-fn]
    (debug "request" request)
    (handler request send-fn)))

(defn log-response-mw [handler]
  (fn [request response]
    (debug "response" response)
    (handler request response)))

(defn eval-ctx-mw [handler {:keys [sci-last-error sci-ctx-atom]}]
  (fn [request send-fn]
    (handler (assoc request
                    :sci-last-error sci-last-error
                    :sci-ctx-atom sci-ctx-atom)
             send-fn)))

(declare ops)

(defn version-string->data [v]
  (assoc (zipmap ["major" "minor" "incremental"]
                 (js->clj (.split v ".")))
         "version-string" v))

(defn handle-describe [request send-fn]
  (send-fn request
           {"versions" {"nbb-nrepl" (version-string->data "TODO")
                        "node" (version-string->data js/process.version)}
            "aux" {}
            "ops" (zipmap (map name (keys ops)) (repeat {}))
            "status" ["done"]}))

(defn the-sci-ns [ctx ns-sym]
  (sci/eval-form ctx (list 'clojure.core/the-ns (list 'quote ns-sym))))

(defn do-handle-eval [{:keys [ns code sci-last-error sci-ctx-atom load-file?] :as request} send-fn]
  (let [prom (with-async-bindings
               {sci/ns ns
                sci/print-length @sci/print-length}
               (nbb/load-string code))]
    (-> prom
        (.then (fn [v]
                 (send-fn request {"value" (pr-str v)
                                   "ns" (str ns)})
                 (send-fn request {"status" ["done"]})))
        (.catch (fn [e]
                  (sci/alter-var-root sci-last-error (constantly e))
                  (let [data (ex-data e)]
                    (when-let [message (or (:message data) (.-message e))]
                      (send-fn request {"err" message}))
                    (send-fn request {"ex" (str e)
                                      "ns" (str (sci/eval-string* @sci-ctx-atom "*ns*"))
                                      "status" ["done"]}))))))
  #_(let [reader (sci/reader code)]
      (try
        (loop [next-val (sci/parse-next @sci-ctx-atom reader)]
          (when-not (= :sci.core/eof next-val)
            (let[result (sci/eval-form @sci-ctx-atom next-val)
                 ns (sci/eval-string* @sci-ctx-atom "*ns*")]
              (when-not load-file?
                (send-fn request {"value" (pr-str result)
                                  "ns" (str ns)}))
              (recur (sci/parse-next @sci-ctx-atom reader)))))
        (send-fn request {"status" ["done"]})
        (catch :default e
          (sci/alter-var-root sci-last-error (constantly e))
          (let [data (ex-data e)]
            (when-let [message (or (:message data) (.-message e))]
              (send-fn request {"err" message}))
            (send-fn request {"ex" (str e)
                              "ns" (str (sci/eval-string* @sci-ctx-atom "*ns*"))
                              "status" ["done"]}))))))

(defn handle-eval [{:keys [ns sci-ctx-atom] :as request} send-fn]
  (do-handle-eval (assoc request :ns (or (when ns
                                           (the-sci-ns @sci-ctx-atom (symbol ns)))
                                         @sci/ns))
                  send-fn))

(defn handle-clone [request send-fn]
  (send-fn request {"new-session" (str (random-uuid))
                    "status" ["done"]}))

(defn handle-close [request send-fn]
  (send-fn request {"status" ["done"]}))

(defn handle-load-file [{:keys [file] :as request} send-fn]
  (do-handle-eval (assoc request
                         :code file
                         :load-file? true
                         :ns @sci/ns)
                  send-fn))

(def ops
  "Operations supported by the nrepl server"
  {:eval handle-eval
   :describe handle-describe
   :clone handle-clone
   :close handle-close
   :load-file handle-load-file})

(defn handle-request [{:keys [op] :as request} send-fn]
  (if-let [op-fn (get ops op)]
    (op-fn request send-fn)
    (do
      (warn "Unhandled operation" op)
      (send-fn request {"status" ["error" "unknown-op" "done"]}))))

(defn make-request-handler [opts]
  (-> handle-request
      coerce-request-mw
      (eval-ctx-mw opts)
      log-request-mw))

(defn make-send-fn [socket]
  (fn [_request response]
    (.write socket (encode response))))

(defn make-reponse-handler [socket]
  (-> (make-send-fn socket)
      log-response-mw
      response-for-mw))

(defn on-connect [opts socket]
  (debug "Connection accepted")
  (.setNoDelay ^node-net/Socket socket true)
  (let [handler (make-request-handler opts)
        response-handler (make-reponse-handler socket)]
    (.on ^node-net/Socket socket "data"
         (fn [data]
           (let [[requests _] (decode-all data :keywordize-keys true)]
             (doseq [request requests]
               (handler request response-handler))))))
  (.on ^node-net/Socket socket "close"
       (fn [had-error?]
         (if had-error?
           (debug "Connection lost")
           (debug "Connection closed")))))

(defn start-server
  "Start nRepl server. Accepts options either as JS object or Clojure map."
  [opts]
  (api/init-require (path/resolve "script.cljs"))
  (let [port (or (:port opts)
                 0)
        _log_level (or (if (object? opts)
                        (.-log_level ^Object opts)
                        (:log_level opts))
                      "info")
        sci-last-error (sci/new-var '*e nil {:ns (sci/create-ns 'clojure.core)})
        ctx-atom nbb/sci-ctx
        server (node-net/createServer
                (partial on-connect {:sci-ctx-atom ctx-atom
                                     :sci-last-error sci-last-error}))]
    ;; Expose "app" key under js/app in the repl
    (.listen server
             port
             (fn []
               (let [port (-> server (.address) .-port)]
                 (info "nRepl server started on port %d . nrepl-cljs-sci version %s"
                       port
                       "TODO")
                 (try
                   (.writeFileSync fs ".nrepl-port" (str port))
                   (catch :default e
                     (warn "Could not write .nrepl-port" e))))))
    server)
  #_(let [onExit (js/require "signal-exit")]
    (onExit (fn [_code _signal]
              (debug "Process exit, removing .nrepl-port")
              (fs/unlinkSync ".nrepl-port")))))

(defn stop-server [server]
  (.close server
          (fn []
            (when (fs/existsSync ".nrepl-port")
              (fs/unlinkSync ".nrepl-port")))))
