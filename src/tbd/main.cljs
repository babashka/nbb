(ns tbd.main
  (:require [tbd.core :as tbd]))

(def fs (js/require "fs"))

(prn :pre-main)

(defn main [& [script-file]]
  (prn :script-file script-file)
  (let [source (str (.readFileSync fs script-file))]
    (tbd/eval-code source)))

