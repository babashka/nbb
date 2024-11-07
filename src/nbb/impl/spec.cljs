(ns nbb.impl.spec
  (:require [nbb.core :as nbb]
            [sci.configs.cljs.spec.alpha :as s]))

(defn init []
  (nbb/register-plugin!
   ::spec
   s/config))
