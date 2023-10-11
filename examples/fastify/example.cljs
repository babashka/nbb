(ns example
  (:require ["fastify$default" :as fastify]
            [nbb.core :refer [await]]))

(defn init
  []
  (let [server (fastify #js {:logger true})]
    (.get server "/" (fn [_ reply]
                       (-> (.send reply "ok")
                           (.status 200))))
    server))

(defn start
  [server]
  (.listen server #js {:host "127.0.0.1" :port 3000}))

(await (-> (init)
           (start)))
