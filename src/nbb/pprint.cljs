(ns nbb.pprint
  (:require [cljs.pprint :as pp]
            [nbb.core :as nbb]
            [nbb.io :as io]
            [sci.core :as sci]))

(def pns (sci/create-ns 'cljs.pprint nil))

(defn pprint [& args]
  (binding [*print-fn* @io/print-fn]
    (apply pp/pprint args)))

(def pprint-namespace
  {'pprint (sci/copy-var pprint pns)})

(defn init []
  (nbb/register-plugin!
   ::pprint
   {:namespaces {'cljs.pprint pprint-namespace}}))



