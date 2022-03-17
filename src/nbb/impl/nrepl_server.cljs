(ns nbb.impl.nrepl-server
  "Original implementation taken from https://github.com/viesti/nrepl-cljs-sci."
  (:require
   ["fs" :as fs]
   ["net" :as node-net]
   ["path" :as path]
   [clojure.string :as str]
   [goog.object :as gobject]
   [nbb.api :as api]
   [nbb.classpath :as cp]
   [nbb.core :as nbb]
   [nbb.impl.bencode :refer [encode decode-all]]
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
    (-> (nbb/eval-next nil (sci/reader code) {:wrap vector})
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


;;;; Completions, based on babashka.nrepl

(defn format [fmt-str x]
  (str/replace fmt-str "%s" x))

(defn fully-qualified-syms [ctx ns-sym]
  (let [syms (sci/eval-string* ctx (format "(keys (ns-map '%s))" ns-sym))
        sym-strs (map #(str "`" %) syms)
        sym-expr (str "[" (str/join " " sym-strs) "]")
        syms (sci/eval-string* ctx sym-expr)
        syms (remove #(str/starts-with? (str %) "nbb.internal") syms)]
    syms))

(defn match [_alias->ns ns->alias query [sym-ns sym-name qualifier]]
  (let [pat (re-pattern query)]
    (or (when (and (= :unqualified qualifier) (re-find pat sym-name))
          [sym-ns sym-name])
        (when sym-ns
          (or (when (re-find pat (str (get ns->alias (symbol sym-ns)) "/" sym-name))
                [sym-ns (str (get ns->alias (symbol sym-ns)) "/" sym-name)])
              (when (re-find pat (str sym-ns "/" sym-name))
                [sym-ns (str sym-ns "/" sym-name)]))))))

(defn ns-imports->completions [sci-ctx-atom query-ns query]
  (let [ctx @sci-ctx-atom
        [_ns-part name-part] (str/split query #"/")
        resolved (sci/eval-string* ctx
                                   (pr-str `(let [resolved# (resolve '~query-ns)]
                                              (when-not (var? resolved#)
                                                resolved#))))]
    (when resolved
      (when-let [[prefix imported] (if name-part
                                     (let [ends-with-dot? (str/ends-with? name-part ".")
                                           fields (str/split name-part #"\.")
                                           fields (if ends-with-dot?
                                                    fields
                                                    (butlast fields))]
                                       [(str query-ns "/" (when (seq fields)
                                                            (let [joined (str/join "." fields)]
                                                              (str joined "."))))
                                        (apply gobject/getValueByKeys resolved
                                               fields)])
                                     [(str query-ns "/") resolved])]
        (let [props (loop [obj imported
                           props []]
                      (if obj
                        (recur (js/Object.getPrototypeOf obj)
                               (into props (js/Object.getOwnPropertyNames obj)))
                        props))
              completions (map (fn [k]
                                 [nil (str prefix k)]) props)]
          completions)))))

(defn handle-complete* [{ns-str :ns
                         :keys [sci-ctx-atom]
                         :as request}]
  (try
    (let [ctx @sci-ctx-atom
          sci-ns (when ns-str
                   (the-sci-ns ctx (symbol ns-str)))]
      (sci/binding [sci/ns (or sci-ns @sci/ns)]
        (if-let [query (or (:symbol request)
                           (:prefix request))]
          (let [has-namespace? (str/includes? query "/")
                query-ns (when has-namespace? (some-> (str/split query #"/")
                                                      first symbol))
                from-current-ns (fully-qualified-syms ctx (sci/eval-string* ctx "(ns-name *ns*)"))
                from-current-ns (map (fn [sym]
                                       [(namespace sym) (name sym) :unqualified])
                                     from-current-ns)
                alias->ns (sci/eval-string* ctx "(let [m (ns-aliases *ns*)] (zipmap (keys m) (map ns-name (vals m))))")
                ns->alias (zipmap (vals alias->ns) (keys alias->ns))
                from-aliased-nss (doall (mapcat
                                         (fn [alias]
                                           (let [ns (get alias->ns alias)
                                                 syms (sci/eval-string* ctx (format "(keys (ns-publics '%s))" ns))]
                                             (map (fn [sym]
                                                    [(str ns) (str sym) :qualified])
                                                  syms)))
                                         (keys alias->ns)))
                all-namespaces (->> (sci/eval-string* ctx "(all-ns)")
                                    (map (fn [ns]
                                           [(str ns) nil :qualified])))
                from-imports (when has-namespace? (ns-imports->completions sci-ctx-atom query-ns query))
                fully-qualified-names (when-not from-imports
                                        (when has-namespace?
                                          (let [ns (get alias->ns query-ns query-ns)
                                                syms (sci/eval-string* ctx (format "(and (find-ns '%s)
                                                                                         (keys (ns-publics '%s)))"
                                                                                   ns))]
                                            (map (fn [sym]
                                                   [(str ns) (str sym) :qualified])
                                                 syms))))
                svs (concat from-current-ns from-aliased-nss all-namespaces fully-qualified-names)
                completions (keep (fn [entry]
                                    (match alias->ns ns->alias query entry))
                                  svs)
                completions (concat completions from-imports)
                completions (->> (map (fn [[namespace name]]
                                        (cond-> {"candidate" (str name)}
                                          namespace (assoc "ns" (str namespace))))
                                      completions)
                                 distinct vec)]
            {"completions" completions
             "status" ["done"]})
          {"status" ["done"]})))
    (catch :default e
      (js/console.warn e)
      {"completions" []
       "status" ["done"]})))

(defn handle-complete [request send-fn]
  (send-fn request (handle-complete* request)))

;;;; End completions

(def ops
  "Operations supported by the nrepl server"
  {:eval handle-eval
   :describe handle-describe
   :clone handle-clone
   :close handle-close
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
