(ns nbb.impl.promesa
  (:require [nbb.core :as nbb]
            [sci.configs.funcool.promesa :as p]))

(prn :promesa2)

(defn init []
  (nbb/register-plugin!
   ::promesa
   p/config))
