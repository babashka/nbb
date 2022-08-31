(ns nbb.pprint
  (:require
   [nbb.core :as nbb]
   [sci.configs.cljs.pprint :refer [config]]))

(defn init []
  (nbb/register-plugin!
   ::pprint
   config))
