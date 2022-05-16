(ns example
  (:require
   ["express$default" :as express]
   [nbb.core :as nbb]
   [reagent.dom.server :refer [render-to-static-markup]]))

(def app (express))
(def port (or js/process.env.PORT 8092))

(defn page []
  (render-to-static-markup
   [:html
    [:body
     [:h1 "Hello world!"]
     [:p (str "This site is running with nbb v" (nbb/version))]]]))

(.get app "/"
      (fn foo [_req res]
        (.send res
               (page))))

(.listen app port
         (fn []
           (println "Example app listening on port" port)))
