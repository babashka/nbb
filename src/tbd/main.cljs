(ns tbd.main
  (:require [tbd.core :as tbd]
            ["fs" :as fs]))

(defn main []
  (let [[_ _ script-file] js/process.argv
        source (str (.readFileSync fs script-file))]
    (tbd/eval_code source)))
