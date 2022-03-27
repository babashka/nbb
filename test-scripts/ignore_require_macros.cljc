(ns ignore-require-macros
  (:require-macros ignore-require-macros))

(defmacro add
  [a b]
  `(+ ~a ~b))

(defn subtract
  [a b]
  (- a b))

{:add (add 2 3)
 :subtract (subtract 3 2)}