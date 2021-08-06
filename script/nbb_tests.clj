(ns nbb-tests
  (:require [babashka.process :refer [process]]
            [clojure.edn :as edn]
            [clojure.test :as t :refer [deftest is testing]]))

(def nl (System/lineSeparator))

(defn nbb* [& args]
  (-> @(process (into ["node" "out/nbb_main.js"] args) {:out :string
                                                        :err :inherit})
      :out))

(defn nbb [& args]
  (edn/read-string (apply nbb* args)))

(deftest expression-test
  (is (= 6 (nbb "-e" "(+ 1 2 3)")))
  (testing "nil doesn't print return value"
    (is (= (str "6" nl) (nbb* "-e" "(prn (+ 1 2 3))")))))

(defn main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))

