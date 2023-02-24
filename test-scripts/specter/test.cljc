(ns specter.test)

(prn 0)

#?(:cljs
   (do (prn 1)
       (prn 2)
       (prn 3)))
