(ns fnbb.utils
  (:require [applied-science.js-interop :as j]
            [reagent.dom.server :refer [render-to-static-markup]]))

(defn get-header
  [req key]
  (let [headers (j/get req :headers)]
    (j/get headers key)))

(defn render
  [& args]
  (apply render-to-static-markup args))
