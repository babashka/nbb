(ns nbb.impl.schema
  (:require
   [nbb.core :as nbb]
   [schema.core]
   [sci.core :as sci]))

(def sns (sci/create-ns 'schema.core nil))

(def schema-namespace
  (sci/copy-ns schema.core sns))

(defn init []
  (nbb/register-plugin!
   ::schema
   {:namespaces {'schema.core schema-namespace}}))

