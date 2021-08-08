(ns nbb.main
  (:require ["module" :refer [createRequire]]
            [clojure.string :as str]
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
        classpath-dirs (cons cwd (str/split classpath (re-pattern nbb/path:delimiter)))
        script-path (when script-file (nbb/path:resolve script-file))
        require (if script-path
                  (createRequire script-path)
                  (createRequire cwd))]
    (set! (.-require goog/global) require)
    (if (or script-path expr)
      (do (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
          (swap! nbb/ctx assoc
                 :require require
                 :classpath {:dirs classpath-dirs})
          ;; (prn :script-dir script-dir)
          (-> (if expr
                (nbb/load-string expr)
                (nbb/load-file script-path))
              (.then (fn [val]
                       (when (and expr (some? val))
                         (prn val))
                       val))
              (.catch (fn [err]
                        (.error js/console (str err))
                        (when (:debug opts)
                          (throw err))))))
      (.error js/console "Usage: nbb <script> or nbb -e <expr>."))))
