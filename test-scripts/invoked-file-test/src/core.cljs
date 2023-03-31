(ns core
  (:require [nbb.core :refer [invoked-file *file*]]))

(defn -main []
  (prn (if (and (some? *file*)
                (= *file* (invoked-file)))
         :invoked
         :not-invoked)))
