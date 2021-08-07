(ns nbb.main-test
  (:require [clojure.test :refer [deftest is async]]
            [nbb.core :as nbb]
            [nbb.main :as main])
  (:require-macros [nbb.test-macros :refer [with-args]]))

(deftest args-test
  (async done
         (with-args ["test-resources/script.cljs"] done
           (.then (main/main)
                  (fn [res]
                    (is (= 6 res)))))))

(deftest eval-string-test
  (is (= 1 1))
  (async done
         (.then (nbb/eval-string nil "(+ 1 2 3)")
                (fn [res]
                  (is (= 6 res))
                  (done))))
  (async done
         (with-args ["test-resources/plet.cljs"] done
           (.then (main/main)
                  (fn [res]
                    (is (= [1 2 "<!DOCTYPE html><html" 1] res)))))))

(deftest parse-args-test
  (is (= {:expr "(+ 1 2 3)"} (main/parse-args ["-e" "(+ 1 2 3)"])))
  (is (= {:script "foo.cljs", :args nil} (main/parse-args ["foo.cljs"])))
  (is (= {:script "foo.cljs", :args '("1" "2" "3")} (main/parse-args ["foo.cljs" "1" "2" "3"]))))
