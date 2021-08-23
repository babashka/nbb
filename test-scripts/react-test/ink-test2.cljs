(ns ink-test
  (:require [reagent.core :as r]
            ["ink" :refer [render Text]]
            ["react" :as react]))

(defn example []
  (let [[count set-count] (react/useState 0)]
    (react/useEffect (fn []
                       (if (< count 1)
                         (let [timer (js/setInterval #(set-count (inc count)) 500)]
                           (fn []
                             (js/clearInterval timer)))
                         (fn []))))
    [:> Text {:color "green"} "Hello, world! " count]))

(defn root []
  [:f> example])

(render (r/as-element [root]))
