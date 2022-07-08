(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   [promesa.core :as p]
   [utils :as u]
   ["ink" :refer [render Text]]
   [reagent.core :as r]))

(def log js/console.log)

(log (chalk/blue "hello"))
(prn (another/cool-fn))

(p/-> (p/delay 1000 (u/util-fn))
      prn)

(defonce state (r/atom 0))

(doseq [n (range 1 11)]
  (js/setTimeout #(swap! state inc) (* n 500)))

(defn hello []
  [:> Text {:color "green"} "Hello, world! " @state])

(render (r/as-element [hello]))
