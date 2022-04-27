(ns nbb-build-tests
  (:require [nbb-tests :refer [nbb make-prog]]
            [clojure.test :as t :refer [is deftest]]))

(deftest datascript-test
  []
  (is (= #{[{:d 2} 2]}
         (nbb "-e"
              (make-prog
               '[(require '[datascript.core :as d])
                 (d/q '[:find ?m ?m-value
                        :in [[?k ?m] ...] ?m-key
                        :where [(get ?m ?m-key) ?m-value]]
                      {:a {:b 1}
                       :c {:d 2}}
                      :d)])))
      "Datascript feature should be enabled"))

(defn main
  []
  (let [{:keys [error fail]} (t/run-tests 'nbb-build-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
