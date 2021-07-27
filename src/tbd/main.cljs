(ns tbd.main
  (:require [tbd.core :as tbd]))

(def fs (js/require "fs"))

(defn main [& [script-file]]
  (let [source (str (.readFileSync fs script-file))]
    (tbd/eval! source)))

