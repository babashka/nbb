(ns script
  (:require [nbb.core :refer [invoked-file *file*]]))

(prn
 (if (= *file* (invoked-file))
   :invoked
   :not-invoked))
