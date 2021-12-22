(ns cljs-bean.test
  (:require [cljs-bean.core :refer [->clj]]))

(def o #js{:x #js {:y 1 :someFn (fn [x] x)}
           :a 1 :b 2 :c 3
           :someFn (fn [x] x)})

(def oc (->clj o))

;; Read
(prn (get oc :x))
(prn (get-in oc [:x :y]))
(prn (select-keys oc [:a :b :c]))

(let [{:keys [x]} oc]
  (prn x))

;; Destructure
(prn (let [{:keys [a b c]} oc]
       [:a a :b b :c c]))

;; Write
(prn (assoc oc :a 2))
(prn (assoc-in oc [:x :y] 100))

(prn (update oc :a inc))
(prn (update-in oc [:x :y] + 10))

;; ;; Call functions
(prn ((:someFn oc) 42))
(prn (.apply (:someFn oc) o #js[42]))

(prn ((get-in oc [:x :someFn]) 42))
(prn (apply (get-in oc [:x :someFn]) #js[42]))
