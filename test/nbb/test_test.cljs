(ns nbb.test-test
  (:require  [clojure.string :as str]
             [clojure.test :as t :refer [is]]
             [nbb.core :as nbb]
             [sci.core :as sci])
  (:require-macros [nbb.macros :refer [with-async-bindings]]
                   [nbb.test-macros :refer [deftest-async]]))

(deftest-async eval-string-test
  (let [output (atom "")]
    (-> (with-async-bindings
          {sci/print-fn (fn [s]
                          (swap! output str s))}
          (nbb/load-string "
    (ns foo (:require [clojure.test :as t :refer [deftest is testing]]))
    (t/deftest foo (t/is (= 1 2))) (t/run-tests 'foo)"))
        (.then (fn [_]
                 (is (str/includes? @output "expected: (= 1 2)  actual: (not (= 1 2))")))))))
