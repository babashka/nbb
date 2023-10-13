(ns fastify-nbb.core
  (:require [fastify-nbb.server :refer [server]]
            [fastify-nbb.config :refer [config]]))

(defn -main
  []
  (.listen server #js {:host (:host config)
                       :port (:port config)})
  nil)
