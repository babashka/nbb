(ns nbb.impl.reagent
  (:require
   [nbb.core :as nbb]
   [sci-configs.reagent.reagent :as r]))

(defn init []
  (nbb/register-plugin!
   ::reagent
   {:namespaces {'reagent.core r/reagent-namespace
                 'reagent.ratom r/reagent-ratom-namespace
                 'reagent.debug r/reagent-debug-namespace}}))
