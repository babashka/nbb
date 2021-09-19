(ns nbb.js-interop
  (:refer-clojure :exclude [let fn defn])
  (:require
   [applied-science.js-interop :as j]
   [applied-science.js-interop.destructure :as d]
   [clojure.core :as c]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(def jns (sci/create-ns 'applied-science.js-interop nil))

(c/defn ^:macro let
  "`let` with destructuring that supports js property and array access.
   Use ^:js metadata on the binding form to invoke. Eg/
   (let [^:js {:keys [a]} obj] …)"
  [_ _ bindings & body]
  (if (empty? bindings)
    `(do ~@body)
    `(~'clojure.core/let ~(vec (d/destructure (take 2 bindings)))
      (~'applied-science.js-interop/let
       ~(vec (drop 2 bindings))
       ~@body))))

(c/defn ^:macro fn
  "`fn` with argument destructuring that supports js property and array access.
   Use ^:js metadata on binding forms to invoke. Eg/
   (fn [^:js {:keys [a]}] …)"
  [_ _ & args]
  (cons 'clojure.core/fn (d/destructure-fn-args args)))

(c/defn ^:macro defn
  "`defn` with argument destructuring that supports js property and array access.
   Use ^:js metadata on binding forms to invoke."
  [_ _ & args]
  (cons 'clojure.core/defn (d/destructure-fn-args args)))

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
   'let (sci/copy-var let jns)
   'fn (sci/copy-var fn jns)
   'defn (sci/copy-var defn jns)
   #_#_'lit (sci/copy-var j/lit jns)})

(c/defn init []
  (nbb/register-plugin!
   ::js-interop
   {:namespaces {'applied-science.js-interop js-interop-namespace}}))

