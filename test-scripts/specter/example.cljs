(ns specter.example
  (:require [com.rpl.specter :refer [transform MAP-VALS MAP-VALS]]))

#_(prn
 (transform [MAP-VALS MAP-VALS]
            inc
            {:a {:aa 1} :b {:ba -1 :bb 2}}))
