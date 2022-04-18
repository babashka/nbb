(ns nbb.impl.data
  (:require [clojure.data :as data]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def data-ns (sci/create-ns 'clojure.data nil))

(def data-namespace
  {'diff (sci/copy-var data/diff data-ns)})

(defn init []
  (nbb/register-plugin!
   ::clojure-data
   {:namespaces {'clojure.data data-namespace}}))
