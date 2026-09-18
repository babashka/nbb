(ns nbb.impl.ffi
  {:no-doc true}
  (:require [babashka.ffi :as ffi]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def fns (sci/create-ns 'babashka.ffi nil))

;; The ClojureScript compiler emits nothing for a defmacro in babashka.ffi, so
;; copy-ns can't copy defcfn and with-open. Reinterpret them here as SCI macros
;; over the expansion functions the namespace exposes for this.
(defn ^:macro defcfn [_ _ name & args]
  (ffi/defcfn-form name args))

(defn ^:macro with-open [_ _ bindings & body]
  (ffi/with-open-form bindings body))

(def ffi-namespace
  (assoc (sci/copy-ns babashka.ffi fns)
         'defcfn (sci/copy-var defcfn fns)
         'with-open (sci/copy-var with-open fns)))

(defn init []
  (nbb/register-plugin!
   ::ffi
   {:namespaces {'babashka.ffi ffi-namespace}}))
