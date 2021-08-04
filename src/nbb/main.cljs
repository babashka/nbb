(ns nbb.main
  (:require ["fs" :as fs]
            ["module" :as mod1 :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]
            [sci.core :as sci]
            [shadow.esm :as esm]))

(defn main []
  
  (let [[_ _ script-file] js/process.argv
        path (when script-file (path/resolve script-file))
        _ (prn :path path)
        require (when path
                  (createRequire path))]
    (.then (esm/dynamic-import "import-meta-resolve")
           (fn [mod]
             (let [resolve (.-resolve mod)]
               (.then (resolve "react" (str "file://" path))
                      (fn [resolved]
                        (prn :res resolved)
                        (esm/dynamic-import resolved))))))
    #_#_(when require
      (set! (.-require goog/global) require))
    (if script-file
      (let [source (str (fs/readFileSync script-file))]
        ;; NOTE: binding doesn't work as expected since eval-code is async.
        ;; Since nbb currently is only called with a script file argument, this suffices
        (sci/alter-var-root nbb/command-line-args (constantly (seq (js/process.argv.slice 3))))
        (nbb/eval-code source require))
      (.error js/console "Nbb expects a script file argument.")) ))
