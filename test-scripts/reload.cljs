(ns reload
  (:require ["./reload.js"] :reload))

(defonce my-atom (atom 0))

(swap! my-atom inc)

(def x js/globalThis.x)

