;; TODO: Delete this file. Only used for testing purposes
(ns nbb.test-redefs
  (:require
   [promesa.core :as p]))

(defn async-func [] (p/delay 1000 :slow-original))

(println "Running tests")

(println async-func)

;; Wrapping with p/do! seems required otherwise the program exits early
(p/do!
 (p/with-redefs [async-func (fn [] (p/resolved :fast-mock))]
   (p/-> (async-func)
         (println))))
