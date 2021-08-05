(ns nbb.main-test
  (:require [nbb.core :as nbb]
            [clojure.test :refer [deftest is testing]]
            ["fs" :as fs]))

(deftest foo-test
  (is (= 1 1))
  (nbb/eval-code {:require nil
                  :script-dir nil} (str (fs/readFileSync "test-resources/script.cljs"))))
