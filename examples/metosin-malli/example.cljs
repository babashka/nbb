(ns example
  (:require [malli.core :as m]))

(prn
 (m/parse
  [:cat [:= :names] [:schema [:* string?]] [:= :nums] [:schema [:* number?]]]
  [:names ["a" "b"] :nums [1 2 3]]))

;;=> [:names ["a" "b"] :nums [1 2 3]]
