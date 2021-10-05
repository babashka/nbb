(ns example
  (:require
   ["@react-pdf/renderer"
    :refer [Page Text View Document StyleSheet] :as pdf]
   [reagent.core :as r]))

(def styles
  (.create StyleSheet
           (clj->js {:page {:flexDirection "row"
                            :backgroundColor "#E4E4E4"}
                     :section {:margin 10
                               :padding 10
                               :flexGrow 1}})))

(defn my-document []
  [:> Document
   [:> Page {:size "A4" :style (.-page styles)}
    [:> View {:style (.-section styles)}
     [:> Text "Section #1"]]
    [:> View {:style (.-section styles)}
     [:> Text "Section #2"]]]])

(.render pdf (r/as-element [my-document]) "example.pdf")
