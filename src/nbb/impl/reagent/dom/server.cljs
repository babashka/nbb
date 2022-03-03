(ns nbb.impl.reagent.dom.server
  (:require
   [nbb.core :as nbb]
   [sci-configs.reagent.reagent-dom-server :as srv]))

(defn init []
  (nbb/register-plugin!
   ::reagent-dom-server
   {:namespaces {'reagent.dom.server srv/reagent-dom-server-namespace}}))
