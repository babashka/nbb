(ns nodashka.main
  (:require [applied-science.js-interop :as j]
            [nodashka.core :as nodashka]
            [shadow.esm :as esm]))

(defn main []
  (let [[_ _ script-file] js/process.argv]
    (if script-file
      (.then (esm/dynamic-import "fs")
             (fn [fs]
               (let [source (str (j/call fs :readFileSync script-file))]
                 (nodashka/eval_code source))))
      (println "Nodashka expects a script file argument.")) ))
