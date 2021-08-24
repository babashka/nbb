(ns example
  (:require ["express$default" :as express]))

(def app (express))
(def port 8092)

(.get app "/"
      (fn foo [_req res]
        (.send res "Hello World!")))

(.listen app port
         (fn []
           (println "Example app listening on port" port)))
