(ns nbb.impl.math
  (:require
   [cljs.math]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(def mns (sci/create-ns 'cljs.math))

(def cljs-math-namespace
  (sci/copy-ns cljs.math mns))

(defn init []
  (nbb/register-plugin!
   ::cljs-math
   {:namespaces {'cljs.math cljs-math-namespace}}))

