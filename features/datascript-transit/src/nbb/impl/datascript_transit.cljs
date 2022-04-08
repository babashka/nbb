(ns nbb.impl.datascript-transit
  {:no-doc true}
  (:require [datascript.transit :as dt]
            [nbb.core :as nbb]
            [sci.core :as sci :refer [copy-var]]))

(def transit-ns (sci/create-ns 'datascript.transit nil))

(def transit-namespace
  {'read-handlers (copy-var dt/read-handlers transit-ns)
   'write-handlers (copy-var dt/write-handlers transit-ns)
   'read-transit-str (copy-var dt/read-transit-str transit-ns)
   'write-transit-str (copy-var dt/write-transit-str transit-ns)})

(defn init []
  (nbb/register-plugin!
   ::datascript-transit
   {:namespaces {'datascript.transit transit-namespace}}))
