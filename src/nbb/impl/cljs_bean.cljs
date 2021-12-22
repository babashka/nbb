(ns nbb.impl.cljs-bean
  (:require
   [cljs-bean.core :as bean]
   [clojure.core :as c]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(def bns (sci/create-ns 'cljs-bean.core nil))

(def cljs-bean-namespace
  {'bean (sci/copy-var bean/bean bns)
   'bean? (sci/copy-var bean/bean? bns)
   'object (sci/copy-var bean/object bns)
   '->js (sci/copy-var bean/->js bns)
   '->clj (sci/copy-var bean/->clj bns)})

(c/defn init []
  (nbb/register-plugin!
   ::cljs-bean
   {:namespaces {'cljs-bean.core cljs-bean-namespace}}))

