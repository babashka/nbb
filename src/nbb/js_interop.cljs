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
   'lookup (sci/copy-var j/lookup jns)
   'assoc! (sci/copy-var j/assoc! jns)
   'assoc-in! (sci/copy-var j/assoc-in! jns)
   'update! (sci/copy-var j/update! jns)
   'update-in! (sci/copy-var j/update-in! jns)
   'extend! (sci/copy-var j/extend! jns)
   'push! (sci/copy-var j/push! jns)
   'unshift! (sci/copy-var j/unshift! jns)
   'call (sci/copy-var j/call jns)
   'apply (sci/copy-var j/apply jns)
   'call-in (sci/copy-var j/call-in jns)
   'apply-in (sci/copy-var j/apply-in jns)
   'obj (sci/copy-var j/obj jns)
   #_#_'lit (sci/copy-var j/lit jns)})

(defn init []
  (nbb/register-plugin!
   ::js-interop
   {:namespaces {'applied-science.js-interop js-interop-namespace}}))

