(ns nbb.main-test
  (:require [clojure.test :refer [deftest is testing async]]
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
         (.then (nbb/load-string "(+ 1 2 3)")
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

(deftest load-string-file-test
  (async done
         (-> (nbb/load-string "(ns foo) (defn foo [] (+ 1 2 3)) (ns-name *ns*)")
             (.then (fn [ns-name]
                      (is (= 'foo ns-name))))
             (.then (fn [_] (nbb/load-string
                             "(nbb.core/load-string \"(ns foo) (defn foo [] (+ 1 2 3)) (ns-name *ns*)\")")))
             (.then (fn [ns-name]
                      (testing "internal load-string"
                        (is (= 'foo ns-name)))))
             (.then (fn [_]
                      (nbb/load-string "(ns-name *ns*)")))
             (.then (fn [ns-name]
                      (is (= 'user ns-name))))
             (.then (fn [_]
                      (nbb/load-file "test-resources/script.cljs")))
             (.then (fn [val]
                      (is (= 6 val))))
             (.then (fn [_]
                      (nbb/load-string "(nbb.core/load-file \"test-resources/script.cljs\")")))
             (.then (fn [val]
                      (is (= 6 val))))
             (.finally (fn [_]
                         (done))))))
