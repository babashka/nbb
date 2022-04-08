(ns nbb.impl.transit
  (:require [cognitect.transit :as transit]
            [nbb.core :as nbb]
            [sci.core :as sci :refer [copy-var]]))

(def transit-ns (sci/create-ns 'cognitect.transit nil))

(def transit-namespace
  {'write (copy-var transit/write transit-ns)
   'writer (copy-var transit/writer transit-ns)
   'write-handler (copy-var transit/write-handler transit-ns)
   'write-meta (copy-var transit/write-meta transit-ns)
   'read (copy-var transit/read transit-ns)
   'reader (copy-var transit/reader transit-ns)
   'read-handler (copy-var transit/read-handler transit-ns)
   'tagged-value (copy-var transit/tagged-value transit-ns)
   'ListHandler (copy-var transit/ListHandler transit-ns)})

(defn init []
  (nbb/register-plugin!
   ::transit
   {:namespaces {'cognitect.transit transit-namespace}}))
