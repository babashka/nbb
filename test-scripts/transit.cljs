(ns transit
  (:require [cognitect.transit :as t]
            [clojure.edn :as edn]))

(defn roundtrip [x]
  (let [w (t/writer :json)
        r (t/reader :json)]
    (t/read r (t/write w x))))

(prn (roundtrip (edn/read-string (first *command-line-args*))))
