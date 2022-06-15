(ns nbb.impl.test
  (:require
   [nbb.core :as nbb]
   [sci.configs.cljs.test :refer [config]]))

(defn init []
  (nbb/register-plugin!
   ::cljs-test
   config))
