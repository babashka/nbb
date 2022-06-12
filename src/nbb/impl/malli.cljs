(ns nbb.impl.malli
  (:require
   [malli.core]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(def sns (sci/create-ns 'malli.core nil))

(def malli-namespace
  (sci/copy-ns malli.core sns))

(defn init []
  (nbb/register-plugin!
   ::malli
   {:namespaces {'malli.core malli-namespace}}))

