(ns js-interop
  (:require [applied-science.js-interop :as j]))

(def o #js{:x #js {:y 1 :someFn (fn [x] x)}
           :a 1 :b 2 :c 3
           :someFn (fn [x] x)})

;; Read
(prn (j/get o :x))
;; (j/get o .-x "fallback-value")
(prn (j/get-in o [:x :y]))
(prn (j/select-keys o [:a :b :c]))

(let [{:keys [x]} (j/lookup o)] ;; lookup wrapper
  (prn x))

;; Destructure
;; (j/let [^:js {:keys [a b c]} o] ...)
;; (j/fn [^:js [n1 n2]] ...)
;; (j/defn my-fn [^:js {:syms [a b c]}])

;; Write
(prn (j/assoc! o :a 1))
(prn (j/assoc-in! o [:x :y] 100))
;; (j/assoc-in! o [.-x .-y] 100)

(prn (j/update! o :a inc))
(prn (j/update-in! o [:x :y] + 10))

;; ;; Call functions
(prn (j/call o :someFn 42))
(prn (j/apply o :someFn #js[42]))

(prn (j/call-in o [:x :someFn] 42))
(prn (j/apply-in o [:x :someFn] #js[42]))

;; ;; Create
(prn (j/obj :a 1 :b 2))
(prn (j/lit {:a 1 :b [2 3 4]}))
