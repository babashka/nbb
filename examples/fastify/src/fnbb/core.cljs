(ns fnbb.core
  (:require [fnbb.server :refer [server]]
            [fnbb.config :refer [config]]))

(defn -main
  []
  (.listen server #js{:host (:host config)
                      :port (:port config)})
  nil)
