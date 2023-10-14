(ns fnbb.plugins.statics
  (:require
   [nbb.core :refer [*file*]]
   ["@fastify/static$default" :as statics]
   ["path" :as path]))

(defn register
  [server _ done]
  (prn "--->>> __filename" *file*)
  (done))
