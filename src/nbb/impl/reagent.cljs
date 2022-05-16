(ns nbb.impl.reagent
  (:require
   [nbb.core :as nbb]
   [reagent.ratom]
   [sci.configs.reagent.reagent :as r]
   [sci.core :as sci]))

(def rns (sci/create-ns 'reagent.ratom))

(defn init []
  (nbb/register-plugin!
   ::reagent
   {:namespaces {'reagent.core r/reagent-namespace
                 'reagent.ratom r/reagent-ratom-namespace
                 'reagent.debug r/reagent-debug-namespace}}))
