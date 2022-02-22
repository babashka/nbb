(ns nbb.impl.nrepl-server
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["fs" :as fs]
   ["net" :as node-net]
   ["path" :as path]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.classpath :as cp]
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

;; TODO: this should not be global
(def last-ns (atom nil))

(defn do-handle-eval [{:keys [ns code sci-last-error _sci-ctx-atom _load-file?] :as request} send-fn]
  (with-async-bindings
    {sci/ns ns
     sci/print-length @sci/print-length
     sci/print-newline true}
    ;; we alter-var-root this because the print-fn may go out of scope in case
    ;; of returned delays
    (sci/alter-var-root sci/print-fn (constantly
                                      (fn [s]
                                        (send-fn request {"out" s}))))
    (-> (nbb/eval-expr nil (sci/reader code) {:wrap vector})
        (.then (fn [v]
                 (let [v (first v)]
                   (reset! last-ns @sci/ns)
                   (send-fn request {"value" (pr-str v)
                                     "ns" (str @sci/ns)}))
                 (send-fn request {"status" ["done"]})))
        (.catch (fn [e]
                  (sci/alter-var-root sci-last-error (constantly e))
                  (let [data (ex-data e)]
                    (when-let [message (or (:message data) (.-message e))]
                      (send-fn request {"err" (str message "\n")}))
                    (send-fn request {"ex" (str e)
                                      "ns" (str @sci/ns)
                                      "status" ["done"]})))))))

(defn handle-eval [{:keys [ns sci-ctx-atom] :as request} send-fn]
  (do-handle-eval (assoc request :ns (or (when ns
                                           (the-sci-ns @sci-ctx-atom (symbol ns)))
                                         @last-ns
                                         @sci/ns))
                  send-fn))

(defn handle-clone [request send-fn]
  (send-fn request {"new-session" (str (random-uuid))
                    "status" ["done"]}))

(defn handle-close [request send-fn]
  (send-fn request {"status" ["done"]}))

(defn
  handle-classpath [request send-fn]
  (send-fn
   request
   {"status" ["done"]
    "classpath"
    (cp/split-classpath (cp/get-classpath))}))

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
   :classpath handle-classpath
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
        response-handler (make-reponse-handler socket)
        pending (atom nil)]
    (.on ^node-net/Socket socket "data"
         (fn [data]
           (let [data (if-let [p @pending]
                        (let [s (str p data)]
                          (reset! pending nil)
                          s)
                        data)
                 [requests unprocessed] (decode-all data :keywordize-keys true)]
             (when (not (str/blank? unprocessed))
               (reset! pending unprocessed))
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
             "127.0.0.1" ;; default for now
             (fn []
               (let [addr (-> server (.address))
                     port (-> addr .-port)
                     host (-> addr .-address)]
                 (println (str "nREPL server started on port " port " on host " host " - nrepl://" host ":" port))
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

(defn
  init
  []
  (let [eval-require (fn
                       [ns-form]
                       (nbb/eval-require
                        (list
                         'quote
                         (list 'quote ns-form))))
        [ns1 ns2] nbb/repl-requires]
    (->
     (eval-require ns1)
     (.then (fn [] (eval-require ns2)))
     (.then (fn [] (start-server @nbb/opts))))))
