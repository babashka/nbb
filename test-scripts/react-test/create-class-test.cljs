(ns create-react-class-test
  (:require [reagent.core :as r]
            [reagent.dom.server :as srv]))

(defn err-boundary
  [& _children]
  (let [err-state (r/atom nil)]
    (r/create-class
      {:component-did-catch (fn [err info]
                              (reset! err-state [err info]))
       :reagent-render (fn [& children]
                         (if (nil? @err-state)
                           (into [:<>] children)
                           (let [[_ info] @err-state]
                             [:p "Oops! Something bad happened : " info])))})))

(prn (srv/render-to-string (r/as-element [:div [err-boundary [:p "Hi"]]])))
