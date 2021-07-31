(ns nbb.main
  (:require [applied-science.js-interop :as j]
            [nbb.core :as nbb]
            [shadow.esm :as esm]))

(defn main []
  (let [[_ _ script-file] js/process.argv]
    (if script-file
      (.then (esm/dynamic-import "fs")
             (fn [fs]
               (let [source (str (j/call fs :readFileSync script-file))]
                 (nbb/eval_code source))))
      (println "Nodashka expects a script file argument.")) ))
