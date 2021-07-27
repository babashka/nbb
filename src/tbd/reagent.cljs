(ns tbd.reagent
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [sci.core :as sci]
            [tbd.core :as tbd]))

(def rns (sci/create-ns 'reagent.core nil))

(def reagent-namespace
  {'atom (sci/copy-var r/atom rns)
   'as-element (sci/copy-var r/as-element rns)})

(def rdns (sci/create-ns 'reagent.dom nil))

(def reagent-dom-namespace
  {'render (sci/copy-var rdom/render rdns)})

(tbd/register-plugin!
 ::reagent
 {:namespaces {'reagent.core reagent-namespace
               'reagent.dom reagent-dom-namespace}})
