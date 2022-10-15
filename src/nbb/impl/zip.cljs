(ns nbb.impl.zip
  (:require
   [clojure.zip]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(def zns (sci/create-ns 'clojure.zip nil))

(def zip-namespace
  (sci/copy-ns clojure.zip zns))

(defn init []
  (nbb/register-plugin!
   ::clojure-zip
   {:namespaces {'clojure.zip zip-namespace}}))

