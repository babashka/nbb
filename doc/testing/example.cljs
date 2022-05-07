(ns example
  (:require
   [cljs.test :as t :refer [async deftest is testing]]
   [promesa.core :as p]))

(deftest awesome-test
  (testing "one equals one"
    (is (= 1 1)))
  (testing "this test will fail"
    (is (= 1 2))))

(deftest awesome-async-test
  (async done
         (-> (p/let [x (p/delay 1000 :done)]
               (is (= :done x)))
             (p/finally done))))

;; print name of each test
(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

;; set exit code based on failing tests
(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (set! (.-exitCode js/process) 1)))

;; run this function with: nbb -m example/run-tests
(defn run-tests []
  (t/run-tests 'example))
