(ns nbb.impl.js-interop
  (:require
   [nbb.core :as nbb]
   [sci.configs.applied-science.js-interop :as j]))

(defn init []
  (nbb/register-plugin!
   ::js-interop
   {:namespaces {'applied-science.js-interop j/js-interop-namespace}}))

