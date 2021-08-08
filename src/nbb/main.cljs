(ns nbb.main
  (:require ["fs" :as fs]
            ["module" :as mod1 :refer [createRequire]]
            ["path" :as path]
            [clojure.string :as str]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(vreset! nbb/fs fs)
(vreset! nbb/path path)

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
        classpath-dirs (cons cwd (str/split classpath (re-pattern path/delimiter)))
        path (when script-file (path/resolve script-file))
        require (if path
                  (createRequire path)
                  (createRequire cwd))]
    (set! (.-require goog/global) require)
    (if (or script-file expr)
      (do (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
          (swap! nbb/ctx assoc
                 :require require :script-dir path
                 :classpath {:dirs classpath-dirs})
          ;; (prn :script-dir script-dir)
          (-> (if expr
                (nbb/load-string expr)
                (nbb/load-file script-file))
              (.then (fn [val]
                       (when (and expr (some? val))
                         (prn val))
                       val))
              (.catch (fn [err]
                        (.error js/console (str err))
                        (when (:debug opts)
                          (throw err))))))
      (.error js/console "Usage: nbb <script> or nbb -e <expr>."))))
