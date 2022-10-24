(ns require-macros
  #?(:clj (:require [required-macros :refer [do2]]))
  #?(:cljs (:require-macros [require-macros :refer [add subtract]]
                            [required-macros :refer [do2]])))

(defmacro add
  [a b]
  `(+ ~a ~b))

(defn subtract
  [a b]
  (- a b))

(do2
 {:add (add 2 3)
  :subtract (subtract 3 2)})
