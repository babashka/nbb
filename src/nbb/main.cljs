(ns nbb.main
  (:require ["module" :as mod1 :refer [createRequire]]
            ["path" :as path]
            [applied-science.js-interop :as j]
            [nbb.core :as nbb]
            [shadow.esm :as esm]))

(defn main []
  (let [[_ _ script-file] js/process.argv
        require (when script-file
                  (let [path (path/resolve script-file)]
                    (prn :path path)
                    (createRequire path)))]
    #_(when require
      (set! (.-require goog/global) require))
    (if script-file
      (.then (esm/dynamic-import "fs")
             (fn [fs]
               (let [source (str (j/call fs :readFileSync script-file))]
                 (nbb/eval-code source require))))
      (println "Nodashka expects a script file argument.")) ))
