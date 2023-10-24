(ns fnbb.plugins.htmx
  (:require [applied-science.js-interop :as j]
            [fnbb.utils :refer [get-header]]))


(defn register
  [server _ done]
  (.decorateRequest server "hx?" false)
  (.addHook server "onRequest" (fn [req _ done]
                                 (let [hx-request (get-header req :hx-request)]
                                   (j/assoc! req :hx? (if (nil? hx-request) false true)))
                                 (done)))
  (done))
