(ns nbb.js-interop
  (:require #_[clojure.core :as c]
            [applied-science.js-interop :as j]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def jns (sci/create-ns 'applied-science.js-interop nil))

(def js-interop-namespace
  {'get (sci/copy-var j/get jns)
   'get-in (sci/copy-var j/get-in jns)
   'contains? (sci/copy-var j/contains? jns)
   'select-keys (sci/copy-var j/select-keys jns)
   'lookup (sci/copy-var j/lookup jns)})

(defn init []
  (nbb/register-plugin!
   ::js-interop
   {:namespaces {'applied-science.js-interop js-interop-namespace}}))

