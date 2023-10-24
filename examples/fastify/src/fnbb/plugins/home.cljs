(ns fnbb.plugins.home
  (:require [fnbb.plugins.layout :refer [layout]]
            [fnbb.utils :refer [render]]))

(defn template
  []
  [:div [:h1 "Home"]
   [:a {:href "/logout"} "Logout"]])

(defn handler-home
  [_ reply]
  (let [html (layout (template) :title "Home")]
    (-> reply
        (.header "content-type" "text/html")
        (.status 200)
        (.send (render html)))))
