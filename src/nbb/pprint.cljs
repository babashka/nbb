(ns nbb.pprint
  (:require [cljs.pprint :as pp]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def pns (sci/create-ns 'cljs.pprint nil))

(defn pprint [& args]
  (binding [*print-fn* @sci/print-fn]
    (apply pp/pprint args)))

(defn print-table [& args]
  (binding [*print-fn* @sci/print-fn]
    (apply pp/print-table args)))

(def pprint-namespace
  {'pprint (sci/copy-var pprint pns)
   'print-table (sci/copy-var print-table pns)})

(defn init []
  (nbb/register-plugin!
   ::pprint
   {:namespaces {'cljs.pprint pprint-namespace}}))
