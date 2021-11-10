(ns nbb.impl.main
  (:require ["path" :as path]
            [clojure.string :as str]
            [nbb.api :as api]
            [nbb.classpath :as cp]
            [nbb.core :as nbb]
            [nbb.error :as error]
            [nbb.impl.common :as common]
            [sci.core :as sci]
            [shadow.esm :as esm]))

(defn parse-args [args]
  (loop [opts {}
         args args]
    (if args
      (let [farg (first args)
            nargs (next args)]
        (case farg
          "-e" (recur (assoc opts :expr (first nargs))
                      (next nargs))
          ("-cp" "--classpath")
          (recur (assoc opts :classpath (first nargs))
                 (next nargs))
          "--debug" (recur (assoc opts :debug true)
                           nargs)
          "nrepl-server" (recur (assoc opts :nrepl-server true)
                                nargs)
          "socket-repl" (recur (assoc opts :socket-repl true)
                               nargs)
          (":port" "--port")
          (recur (assoc opts :port (first nargs))
                 (next nargs))
          "repl" (recur (assoc opts :repl true)
                        nargs)
          ;; default
          (if (not (:expr args))
            ;; when not expression, this argument is interpreted as file
            (assoc opts :script farg :args (next args))
            (throw (ex-info (str "Unrecognized options:"  args) {})))))
      opts)))

(defn main []
  (let [[_ _ & args] js/process.argv
        opts (parse-args args)
        _ (reset! common/opts opts)
        script-file (:script opts)
        expr (:expr opts)
        classpath (:classpath opts)
        cwd (js/process.cwd)
        _ (do (cp/add-classpath cwd)
              (when classpath (cp/add-classpath classpath)))
        nrepl-server (:nrepl-server opts)
        repl? (or (:repl opts)
                  (:socket-repl opts)
                  ;; TODO: better handling of detecting invocation without subtask
                  (empty? (dissoc opts :expr :classpath :debug)))]
    (reset! nbb/opts opts)
    (when repl? (api/init-require (path/resolve "script.cljs")))
    (if (or script-file expr nrepl-server repl?)
      (do (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
          (-> (cond script-file
                    (api/loadFile script-file)
                    expr
                    (api/loadString expr)
                    (:nrepl-server opts)
                    (esm/dynamic-import "./nbb_nrepl_server.js")
                    (and repl? (:socket-repl opts))
                    (-> (esm/dynamic-import "./nbb_repl.js")
                        (.then (fn [_mod]
                                 ((-> nbb/sci-ctx deref :env deref
                                      :namespaces (get 'nbb.repl) (get 'socket-repl))
                                  {:port (:port opts)}))))
                    repl?
                    (-> (esm/dynamic-import "./nbb_repl.js")
                        (.then (fn [_mod]
                                 ((-> nbb/sci-ctx deref :env deref
                                      :namespaces (get 'nbb.repl) (get 'repl)))))))
              (.then (fn [val]
                       (when (and expr (some? val))
                         (prn val))
                       val))
              (.catch (fn [err]
                        (error/error-handler err opts)
                        (when (:debug opts)
                          (.error js/console (str err)))
                        (throw (js/Error. (ex-message err)))))))
      (.error js/console "Usage: nbb <script> or nbb -e <expr>."))))

(defn init [])
