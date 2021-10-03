(ns nbb.impl.reagent.dom.server
  (:require
   [nbb.core :as nbb]
   [reagent.dom.server :as srv]
   [sci.core :as sci]))

(def rdns (sci/create-ns 'reagent.dom.server nil))

(def reagent-dom-server-namespace
  {'render-to-string (sci/copy-var srv/render-to-string rdns)
   'render-to-static-markup (sci/copy-var srv/render-to-static-markup rdns)})

(defn init []
  (nbb/register-plugin!
   ::reagent-dom-server
   {:namespaces {'reagent.dom.server reagent-dom-server-namespace}}))
