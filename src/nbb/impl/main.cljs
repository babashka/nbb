(ns nbb.impl.main
  (:require
   ["path" :as path]
   [babashka.cli :as cli]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.classpath :as cp]
   [nbb.core :as nbb]
   [nbb.error :as error]
   [nbb.impl.common :as common]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [shadow.esm :as esm]))

(defn main-expr [main-fn]
  (let [main-fn (symbol main-fn)
        main-fn (if (simple-symbol? main-fn)
                  (symbol (str main-fn) "-main")
                  main-fn)
        ns (namespace main-fn)
        expr (str/replace "(require '$1) (apply $2 *command-line-args*)"
                          #"\$(\d)"
                          (fn [match]
                            (case (second match)
                              "1" ns
                              "2" main-fn)))]
    expr))

(defn parse-args [args]
  (loop [opts {}
         args args]
    (if args
      (let [farg (first args)
            nargs (next args)]
        (case farg
          ("--help" "-h") (assoc opts :help true)
          ("--version" "-v") (assoc opts :version true)
          "-e" (recur (assoc opts :expr (first nargs))
                      (next nargs))
          ("-m" "--main")
          (assoc opts
                 :expr (main-expr (first nargs))
                 :args (next nargs))
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
          (":host" "--host")
          (recur (assoc opts :host (first nargs))
                 (next nargs))
          "repl" (recur (assoc opts :repl true)
                        nargs)
          "bundle" (assoc opts :bundle-opts (cli/parse-args nargs
                                                            {:aliases {:o :out
                                                                       :h :help}}))
          ;; default
          (if (not (:expr args))
            ;; when not expression, this argument is interpreted as file
            (assoc opts :script farg :args (next args))
            (throw (ex-info (str "Unrecognized options:"  args) {})))))
      opts)))

(defn print-help []
  (println (str/capitalize (nbb/cli-name)) "version:" (nbb/version))
  (println "
Help:

 -h / --help: print this help text and exit.
 -v / --version: print the current version of nbb.

Global options:

 --debug: print additional debug info.
 -cp / --classpath: set the classpath.

Evaluation:

 -e: execute expression.
 -m / --main: execute main function.

REPL:

 repl: start console REPL.
 nrepl-server: start nrepl server. [1,2]
 socket-repl: start socket repl server. [1]

 1: Provide :port <port> to specify port.
 2: Provide :host <host> to specify host.

Tooling:

  bundle: produce single JS file for usage with bundlers.
"))

(defn main []
  (let [[_ _ & args] js/process.argv
        opts (parse-args args)
        _ (reset! common/opts opts)
        script-file (:script opts)
        expr (:expr opts)
        classpath (:classpath opts)
        cwd (js/process.cwd)
        _ (do (cp/add-classpath cwd)
              (when classpath
                (cp/add-classpath classpath)))
        nrepl-server (:nrepl-server opts)
        repl? (or (:repl opts)
                  (:socket-repl opts)
                  ;; TODO: better handling of detecting invocation without subtask
                  (empty? (dissoc opts :classpath :debug)))
        bundle-opts (:bundle-opts opts)]
    (when (:help opts)
      (print-help)
      (js/process.exit 0))
    (when (:version opts)
      (println (str (nbb/cli-name) " v" (nbb/version)))
      (js/process.exit 0))
    (reset! nbb/opts opts)
    (when repl? (api/init-require (path/resolve "script.cljs")))
    (if (or script-file expr nrepl-server repl? bundle-opts)
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
                                 ((-> (store/get-ctx) :env deref
                                      :namespaces (get 'nbb.repl) (get 'socket-repl))
                                  {:port (:port opts)}))))
                    (:bundle-opts opts)
                    (esm/dynamic-import "./nbb_bundler.js")
                    repl?
                    (-> (esm/dynamic-import "./nbb_repl.js")
                        (.then (fn [_mod]
                                 ((-> (store/get-ctx) :env deref
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
      (.error js/console (str "Usage: " (nbb/cli-name) " <script> or nbb -e <expr>.")))))

(defn init [])
