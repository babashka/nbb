(ns ink-demo
  (:require ["ink" :refer [render Text]]
            [reagent.core :as r]))

(defonce state (r/atom 0))

(doseq [n (range 1 11)]
  (js/setTimeout #(swap! state inc) (* n 500)))

(defn hello []
  (r/with-let [x (r/atom 0)
               _ (js/setInterval #(swap! x inc) 1000)]
    [:<>
     [:> Text {:color "green"} "Counting to 10: " @state]
     [:> Text {:color "yellow"} "Seconds since launch: " @x]]))

(render (r/as-element [hello]))
