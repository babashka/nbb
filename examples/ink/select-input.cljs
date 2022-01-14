(ns script
  (:require ["ink" :refer [render]]
            ["ink-select-input$default.default" :as SelectInput]
            [reagent.core :as r]))

(def items (clj->js [{:label "First"
                      :value "first"}
                     {:label "Second"
                      :value "second"}]))

(declare ink-state)

(defn handle-select [i]
  ((.-clear ink-state))
  ((.-unmount ink-state))
  (js/console.log i)
  (js/process.exit 0))

(defn select []
  [:> SelectInput {:items items :onSelect handle-select}])

(def ink-state (render (r/as-element [select])))
