(ns example
  (:require
   ["@nut-tree/nut-js" :as nut-js :refer [mouse left right up down]]
   [promesa.core :as p]))

(p/do
  (mouse.move (left 500))
  (mouse.move (up 500))
  (mouse.move (right 500))
  (mouse.move (down 500)))
