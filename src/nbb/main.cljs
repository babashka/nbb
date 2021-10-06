(ns nbb.main
  (:require ["path" :as path]
            [clojure.string :as str]
            [nbb.api :as api]
            [nbb.core :as nbb]
            [nbb.error :as error]
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
          (":port" "--port")
          (recur (assoc opts :port (first nargs))
                 (next nargs))
          ;; default
          (if (not (:expr args))
            ;; when not expression, this argument is interpreted as file
            (assoc opts :script farg :args (next args))
            (throw (ex-info (str "Unrecognized options:"  args) {})))))
      opts)))

(defn main []
  (let [[_ _ & args] js/process.argv
        opts (parse-args args)
        script-file (:script opts)
        expr (:expr opts)
        classpath (:classpath opts)
        cwd (js/process.cwd)
        classpath-dirs (cons cwd (str/split classpath (re-pattern path/delimiter)))
        nrepl-server (:nrepl-server opts)]
    (reset! nbb/opts opts)
    (if (or script-file expr nrepl-server)
      (do (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
          (swap! nbb/ctx assoc :classpath {:dirs classpath-dirs})
          (-> (cond script-file
                    (api/loadFile script-file)
                    expr
                    (api/loadString expr)
                    (:nrepl-server opts)
                    (esm/dynamic-import "./nbb_nrepl_server.js"))
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

;; needed on node 14 which is used in CI
(js/process.on
 "unhandledRejection"
 (fn [err]
   (.error js/console (ex-message err))
   (set! (.-exitCode  js/process) 1)))
