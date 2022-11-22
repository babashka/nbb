(ns example
  (:require ["redis$default" :as redis]
            [nbb.core :as nbb]
            [promesa.core :as p]))

(def client
  "initialize the redis client"
  (nbb/await
   (redis/createClient
    #js{:url "redis://127.0.0.1:6379"})))

(nbb/await
 (p/let [db client]
   (.connect db)
   (p/do
     ;; operations on redis (in this case, set and get)
     (.set db "my-key" (js/JSON.stringify (clj->js {:a 1 :b 2})))
     (p/then (.get db "my-key") prn))
   ;; close the connection after performing operations
   (.disconnect db)))
