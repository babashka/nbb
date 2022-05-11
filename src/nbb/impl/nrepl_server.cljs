(ns nbb.impl.nrepl-server
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["fs" :as fs]
   ["net" :as node-net]
   ["path" :as path]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.classpath :as cp]
   [goog.string :as gstring]
   [goog.string.format]
   [nbb.core :as nbb]
   [nbb.impl.bencode :refer [encode decode-all]]
   [nbb.impl.repl-utils :as utils :refer [the-sci-ns]]
   [sci.core :as sci])
  (:require-macros
   [nbb.macros :refer [with-async-bindings]]))

(defn debug [& strs]
  (when (:debug @nbb/opts)
    (.debug js/console (str/join " " strs))))

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

(defn eval-ctx-mw [handler {:keys [sci-ctx-atom]}]
  (fn [request send-fn]
    (handler (assoc request
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

;; TODO: this should not be global
(def last-ns (atom nil))

(def pretty-print-fns-map
  {"clojure.core/prn" prn
   "clojure.pprint/pprint" pp/pprint
   "cider.nrepl.pprint/pprint" pp/pprint})

(defn format-value [nrepl-pprint pprint-options value]
  (if nrepl-pprint
    (if-let [pprint-fn (pretty-print-fns-map nrepl-pprint)]
      (let [{:keys [right-margin length level]} pprint-options]
        (binding [*print-length* length
                  *print-level* level
                  pp/*print-right-margin* right-margin]
          (with-out-str (pprint-fn value))))
      (do
        (debug "Pretty-Printing is only supported for clojure.core/prn and clojure.pprint/pprint.")
        (pr-str value)))
    (pr-str value)))

(defn do-handle-eval [{:keys [ns code sci-last-error file
                              _sci-ctx-atom _load-file? _line] :as request} send-fn]
  (with-async-bindings
    {sci/ns ns
     sci/file file
     sci/print-length @sci/print-length
     sci/print-newline true}
    ;; we alter-var-root this because the print-fn may go out of scope in case
    ;; of returned delays
    (sci/alter-var-root sci/print-fn (constantly
                                      (fn [s]
                                        (send-fn request {"out" s}))))
    (-> (nbb/eval-next nil (sci/reader code) {:wrap vector})
        (.then (fn [v]
                 (let [v (first v)]
                   (reset! last-ns @sci/ns)
                   (sci/alter-var-root sci/*3 (constantly @sci/*2))
                   (sci/alter-var-root sci/*2 (constantly @sci/*1))
                   (sci/alter-var-root sci/*1 (constantly v))
                   (let [v (format-value (:nrepl.middleware.print/print request)
                                         (:nrepl.middleware.print/options request)
                                         v)]
                     (send-fn request {"value" v
                                       "ns" (str @sci/ns)})))
                 (send-fn request {"status" ["done"]})))
        (.catch (fn [e]
                  (sci/alter-var-root sci/*e (constantly e))
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

(defn
  handle-macroexpand
  [{:keys [code expander] :as request} send-fn]
  (try
    (handle-eval
     (assoc
      request
      :code
      (str "( " expander "(quote "  code  "))"))
     (fn
       [request response]
       (send-fn
        request
        (if-let [value (response "value")]
          (assoc response "expansion" value)
          response))))
    (catch js/Error e
      (send-fn request {"ex" (str e)
                        "ns" (str @sci/ns)
                        "status" ["done"]}))))


;; compare [[babashka.nrepl.impl.server]]

(defn forms-join [forms]
  (->> (map pr-str forms)
       (str/join \newline)))

(defn handle-lookup [{:keys [sci-ctx-atom ns] :as request} send-fn]
  (let [mapping-type (-> request :op)]
    (try
      (let [ns-str (:ns request)
            sym-str (or (:sym request) (:symbol request))
            sci-ns
            (or (when ns
                  (the-sci-ns @sci-ctx-atom (symbol ns)))
                @last-ns
                @sci/ns)]
        (sci/binding [sci/ns sci-ns]
          (let [m (sci/eval-string* @sci-ctx-atom (gstring/format "
(let [ns '%s
      full-sym '%s]
  (when-let [v (ns-resolve ns full-sym)]
    (let [m (meta v)]
      (assoc m :arglists (:arglists m)
       :doc (:doc m)
       :name (:name m)
       :ns (some-> m :ns ns-name)
       :val @v))))" ns-str sym-str))
                doc (:doc m)
                file (:file m)
                line (:line m)
                reply (case mapping-type
                        :eldoc (cond->
                                   {"ns" (:ns m)
                                    "name" (:name m)
                                    "eldoc" (mapv #(mapv str %) (:arglists m))
                                    "type" (cond
                                             (ifn? (:val m)) "function"
                                             :else "variable")
                                    "status" ["done"]}
                                   doc (assoc "docstring" doc))
                        (:info :lookup) (cond->
                                            {"ns" (:ns m)
                                             "name" (:name m)
                                             "arglists-str" (forms-join (:arglists m))
                                             "status" ["done"]}
                                            doc (assoc "doc" doc)
                                            file (assoc "file" file)
                                            line (assoc "line" line)))]
            (send-fn request reply))))
      (catch js/Error e
        (let [status (cond->
                         #{"done"}
                         (= mapping-type :eldoc)
                         (conj "no-eldoc"))]
          (send-fn
           request
           {"status" status "ex" (str e)}))))))

(defn handle-load-file [{:keys [file] :as request} send-fn]
  (do-handle-eval (assoc request
                         :code file
                         :load-file? true
                         :ns @sci/ns)
                  send-fn))


;;;; Completions, based on babashka.nrepl

(defn handle-complete [request send-fn]
  (send-fn request (utils/handle-complete* request)))

;;;; End completions

(def ops
  "Operations supported by the nrepl server"
  {:eval handle-eval
   :describe handle-describe
   :info handle-lookup
   :lookup handle-lookup
   :eldoc handle-lookup
   :clone handle-clone
   :close handle-close
   :macroexpand handle-macroexpand
   :classpath handle-classpath
   :load-file handle-load-file
   :complete handle-complete})

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
        host (or (:host opts)
                 "127.0.0.1" ;; default
                 )
        _log_level (or (if (object? opts)
                         (.-log_level ^Object opts)
                         (:log_level opts))
                       "info")
        ctx-atom nbb/sci-ctx
        server (node-net/createServer
                (partial on-connect {:sci-ctx-atom ctx-atom}))]
    ;; Expose "app" key under js/app in the repl
    (.listen server
             port
             host
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
