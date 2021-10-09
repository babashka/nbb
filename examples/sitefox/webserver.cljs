(ns webserver
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let
                                 webserver/defp clojure.core/def}}}
  (:require
   [promesa.core :as p]
   [reagent.dom.server :refer [render-to-static-markup] :rename {render-to-static-markup r}]
   [sitefox.web :as web]))

(defn setup-routes [app]
  (web/reset-routes app)
  (.get app "/"
        (fn [_req res] (.send res (r [:h1 "Hello world!"])))))

(p/let [app (web/create)
        [_host _port] (web/serve app)]
  (setup-routes app)
  (println "Serving."))

;;;; Scratch

(comment
  (defmacro defp [binding expr]
    `(let [v# ~expr]
       (if (instance? js/Promise v#)
         (.then (fn [val]
                  (def ~binding val)))
         (def ~binding v#))))

  (defp app (web/create))
  (web/serve app)
  (setup-routes app)
  )
