(ns nbb.impl.promesa
  (:require [nbb.core :as nbb]
            [sci.configs.funcool.promesa :as p]))

(defn init []
  (nbb/register-plugin!
   ::promesa
   p/config))
