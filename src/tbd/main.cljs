(ns tbd.main
  (:require [applied-science.js-interop :as j]
            [shadow.esm :as esm]
            [tbd.core :as tbd]))

(defn main []
  (let [[_ _ script-file] js/process.argv]
    (.then (esm/dynamic-import "fs")
           (fn [fs]
             (let [source (str (j/call fs :readFileSync script-file))]
               (tbd/eval_code source)))) ))
