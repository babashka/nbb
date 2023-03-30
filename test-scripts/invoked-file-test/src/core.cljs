(ns core
  (:require [nbb.core :refer [invoked-file *file*]]))

(defn -main []
  (prn (if (= *file* (invoked-file))
     :invoked
     :not-invoked)))
