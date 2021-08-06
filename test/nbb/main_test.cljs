(ns nbb.main-test
  (:require [clojure.test :refer [deftest is async]]
            [nbb.core :as nbb]
            [nbb.main :as main])
  (:require-macros [nbb.test-macros :refer [with-args]]))

(deftest eval-string-test
  (is (= 1 1))
  (async done
         (.then (nbb/eval-string nil "(+ 1 2 3)")
                (fn [res]
                  (is (= 6 res))
                  (done)))))

(deftest args-test
  (async done
         (with-args ["test-resources/script.cljs"] done
           (.then (main/main)
                  (fn [res]
                    (is (= 6 res)))))))
