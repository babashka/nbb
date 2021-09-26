(ns nbb.main
  (:require ["path" :as path]
            [clojure.string :as str]
            [nbb.api :as api]
            [nbb.core :as nbb]
            [sci.core :as sci]))

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
        classpath-dirs (cons cwd (str/split classpath (re-pattern path/delimiter)))]
    (if (or script-file expr)
      (do (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
          (swap! nbb/ctx assoc :classpath {:dirs classpath-dirs})
          (-> (if script-file
                (api/loadFile script-file)
                (api/loadString expr))
              (.then (fn [val]
                       (when (and expr (some? val))
                         (prn val))
                       val))
              (.catch (fn [err]
                        (when-let [st (sci/stacktrace err)]
                          (run! #(.error js/console %) (sci/format-stacktrace st)))
                        (.error js/console (str err))
                        (when (:debug opts)
                          (throw err))))))
      (.error js/console "Usage: nbb <script> or nbb -e <expr>."))))
