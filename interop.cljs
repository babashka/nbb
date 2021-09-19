(ns interop
  (:require [applied-science.js-interop :as j]))

(prn (j/get #js{:a 1} :a))
