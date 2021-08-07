(ns nbb.main
  (:require ["fs" :as fs]
            ["module" :as mod1 :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(defn parse-args [args]
  (loop [opts {}
         args args
         parsed-opts? false]
    (if args
      (let [farg (first args)
            nargs (next args)]
        (case farg
          "-e" (recur (assoc opts :expr (first nargs))
                      (next nargs)
                      true)
          ;; default
          (if (not parsed-opts?)
            (assoc opts :script farg :args (next args))
            (throw (ex-info (str "Unrecognized options:"  args) {})))))
      opts)))

(defn main []
  (let [[_ _ & args] js/process.argv
        opts (parse-args args)
        script-file (:script opts)
        expr (:expr opts)
        path (when script-file (path/resolve script-file))
        require (when path
                  (createRequire path))]
    (when require
      (set! (.-require goog/global) require))
    (if (or script-file expr)
      (let [source (or expr (str (fs/readFileSync script-file)))]
        ;; NOTE: binding doesn't work as expected since eval-code is async.
        ;; Since nbb currently is only called with a script file argument, this suffices
        (sci/alter-var-root nbb/command-line-args (constantly (:args opts)))
        (swap! nbb/ctx assoc :require require :script-dir path)
        (-> (nbb/load-string source)
            (.then (fn [val]
                     (when (and expr (some? val))
                       (prn val))
                     val))))
      (.error js/console "Usage: nbb <script> or nbb -e <expr>."))))
