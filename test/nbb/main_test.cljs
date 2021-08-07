(ns nbb.main-test
  (:require [clojure.test :refer [deftest is testing async]]
            [nbb.core :as nbb]
            [nbb.main :as main]))

;; NOTE: CLJS only accepts one async + done per deftest
;; See https://clojurescript.org/tools/testing#async-testing.

(defn with-args [args f]
  (let [old-args js/process.argv
        args (into-array (list* nil nil args))]
    (set! (.-argv js/process) args)
    (-> (f)
        (js/Promise.resolve)
        (.finally (fn []
                    (set! (.-argv js/process) old-args))))))

(defn main-with-args [args]
  (with-args args #(main/main)))

(deftest parse-args-test
  (is (= {:expr "(+ 1 2 3)"} (main/parse-args ["-e" "(+ 1 2 3)"])))
  (is (= {:script "foo.cljs", :args nil} (main/parse-args ["foo.cljs"])))
  (is (= {:script "foo.cljs", :args '("1" "2" "3")} (main/parse-args ["foo.cljs" "1" "2" "3"])))
  (is (= {:classpath "src", :script "foo.cljs", :args nil} (main/parse-args ["--classpath" "src" "foo.cljs"]))))

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
                      (nbb/load-file "test_resources/script.cljs")))
             (.then (fn [val]
                      (is (= 6 val))))
             (.then (fn [_]
                      (nbb/load-string "(nbb.core/load-file \"test_resources/script.cljs\")")))
             (.then (fn [val]
                      (is (= 6 val))))
             (.finally (fn [] (done))))))

(deftest args-test
  (async done
         (-> (main-with-args ["test_resources/script.cljs"])
             (.then (fn [res]
                      (is (= 6 res))))
             (.then (fn [_]
                      (main-with-args ["-e" "(+ 1 2 3 4)"])))
             (.then (fn [res]
                      (is (= 10 res))))
             (.then (fn [_]
                      (main-with-args["-e" "(nbb.core/load-file \"test_resources/script.cljs\")"])))
             (.then (fn [res]
                      (is (= 6 res))))
             (.then (fn [_]
                      (main-with-args ["test_resources/load_file_test.cljs"])))
             (.then (fn [res]
                      (is (= :loaded-by-load-file-test/loaded res))))
             (.finally (fn [] (done))))))

(deftest eval-string-test
  (async done
         (-> (nbb/load-string "(+ 1 2 3)")
             (.then (fn [res]
                      (is (= 6 res))))
             (.then (fn [_]
                      (main-with-args ["test_resources/plet.cljs"])))
             (.then (fn [res]
                      (is (= [1 2 "<!DOCTYPE html><html" 1] res))))
             (.finally (fn [] (done))))))

(deftest require-namespace-test
  (async done
         (-> (main-with-args ["-e" "(require '[test-resources.script :as s])
                                    (s/script-fn)"])
             (.then (fn [res]
                      (is (= :hello res))))
             (.finally (fn [] (done))))))
