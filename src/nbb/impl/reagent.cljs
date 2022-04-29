(ns nbb.impl.reagent
  (:require
   [nbb.core :as nbb]
   [reagent.core :as re]
   [reagent.ratom]
   [sci-configs.reagent.reagent :as r]
   [sci.core :as sci]))

(def rns (sci/create-ns 'reagent.ratom))

(defn init []
  (nbb/register-plugin!
   ::reagent
   {:namespaces {'reagent.core (assoc r/reagent-namespace
                                      'reactify-component (sci/copy-var re/reactify-component rns))
                 'reagent.ratom (assoc r/reagent-ratom-namespace
                                       ;; TODO: move the below to sci configs
                                       'atom (sci/copy-var reagent.ratom/atom
                                                           rns)
                                       'make-reaction (sci/copy-var reagent.ratom/make-reaction
                                                                    rns)
                                       'track! (sci/copy-var reagent.ratom/track! rns))
                 'reagent.debug r/reagent-debug-namespace}}))
