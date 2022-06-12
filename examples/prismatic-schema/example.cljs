(ns example
  (:require [schema.core :as s :include-macros true]))

(s/set-fn-validation! true)
(s/validate {:a s/Int} {:a 1})
; This will make things break:
(s/validate {:a s/Int} {:a "2"})
