(ns nbb.main
  (:require ["fs" :as fs]
            ["module" :as mod1 :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(defn main []
  (let [[_ _ script-file] js/process.argv
        path (when script-file (path/resolve script-file))
        require (when path
                  (createRequire path))]
    (when require
      (set! (.-require goog/global) require))
    (if script-file
      (let [source (str (fs/readFileSync script-file))]
        ;; NOTE: binding doesn't work as expected since eval-code is async.
        ;; Since nbb currently is only called with a script file argument, this suffices
        (sci/alter-var-root nbb/command-line-args (constantly (seq (js/process.argv.slice 3))))
        (nbb/eval-code {:require require
                        :script-dir path} source))
      (.error js/console "Nbb expects a script file argument."))))
