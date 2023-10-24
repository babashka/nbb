(ns fnbb.plugins.statics
  (:require
   [nbb.core :refer [*file*]]
   ["@fastify/static$default" :as statics]
   ["path" :as path]))


(def __filename *file*)

(def __dirname (.dirname path __filename))

(def statics-dirpath (.resolve path __dirname "../public"))

(defn register
  [server _ done]
  (.register server statics #js {:root statics-dirpath
                                 :prefix "/public/"})
  (done))
