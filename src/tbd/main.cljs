(ns tbd.main
  (:require [tbd.core :as tbd]
#_            ["fs" :as fs]))

(prn :pre-main)

(defn main [& [script-file]]
  (prn :script-file script-file)
  #_(let [source (str (.readFileSync fs script-file))]
    (tbd/eval_code source)))

