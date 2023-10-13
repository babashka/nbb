(ns fastify-nbb.plugins.home
  (:require [fastify-nbb.plugins.layout :refer [layout]]
            [fastify-nbb.utils :refer [render]]))

(defn template
  []
  [:div [:h1 "Home"]])

(defn handler-home
  [_ reply]
  (let [html (layout (template) :title "Home")]
    (-> reply
        (.header "content-type" "text/html")
        (.status 200)
        (.send (render html)))))
