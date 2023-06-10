(ns task-list
  (:require [reagent.core :as r]
            [promesa.core :as p]
            ["ink" :as ink]
            ["ink-task-list" :refer [TaskList Task]]
            ["cli-spinners$default" :as cli-spinners]))

(defn transition! [app-state]
  (cond
    (= (get-in @app-state [:tasks "one" :state]) "pending")
    (swap! app-state update-in [:tasks "one"] merge {:state "loading" :status "woo!"})

    (= (get-in @app-state [:tasks "two" :state]) "pending")
    (swap! app-state update-in [:tasks "two"] merge {:state "loading" :status "yay!"})

    (= (get-in @app-state [:tasks "three" :state]) "pending")
    (swap! app-state update-in [:tasks "three"] merge {:state "loading" :status "awesome!"})

    (= (get-in @app-state [:tasks "one" :state]) "loading")
    (swap! app-state update-in [:tasks "one"] merge {:state "success" :status "done"})

    (= (get-in @app-state [:tasks "two" :state]) "loading")
    (swap! app-state update-in [:tasks "two"] merge {:state "error" :status "oopsy"})

    (= (get-in @app-state [:tasks "three" :state]) "loading")
    (swap! app-state update-in [:tasks "three"] merge {:state "success" :status "done"})

    :else
    (do
      (js/clearTimeout (:interval-handle @app-state))
      (p/resolve! (:done-promise @app-state) :done))))

(defn task [app-state task-id]
 [:> Task @(r/cursor app-state [:tasks task-id])])

; npm install
; npx nbb -m task-list
(defn -main [& args]
  (p/let [app-state (r/atom {:tasks           {"one"   {:label "one" :state "pending" :status "" :spinner (.-dots cli-spinners)}
                                               "two"   {:label "two" :state "pending" :status "" :spinner (.-dots cli-spinners)}
                                               "three" {:label "three" :state "pending" :status "" :spinner (.-dots cli-spinners)}}
                             :done-promise    (p/deferred)
                             :interval-handle nil})
          ink-state (ink/render (r/as-element [:> TaskList (for [task-id (-> @app-state :tasks keys)]
                                                             ^{:key task-id} [task app-state task-id])]))
          _         (swap! app-state assoc :interval-handle (js/setInterval transition! 1000 app-state))
          done?     (:done-promise @app-state)]
    ((.-unmount ink-state))
    (js/process.exit 0)))

