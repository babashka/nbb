(ns nbb.impl.reagent
  (:require
   [nbb.core :as nbb]
   [reagent.core :as reagent]
   [reagent.ratom]
   [sci.configs.reagent.reagent :as r]
   [sci.core :as sci]))

(defn init []
  (nbb/register-plugin!
   ::reagent
   {:namespaces {'reagent.core (assoc r/reagent-namespace 'adapt-react-class
                                      (sci/copy-var reagent/adapt-react-class (sci/create-ns 'reagent.core)))
                 'reagent.ratom r/reagent-ratom-namespace
                 'reagent.debug r/reagent-debug-namespace}}))
