(ns nbb.test-test
  (:require  [clojure.string :as str]
             [clojure.test :as t :refer [deftest is async]]
             [nbb.core :as nbb]
             [sci.core :as sci])
  (:require-macros [nbb.macros :refer [with-async-bindings]]
                   [nbb.test-macros :refer [deftest-async]]))

(deftest-async deftest-test
  (let [output (atom "")]
    (-> (with-async-bindings
          {sci/print-fn (fn [s]
                          (swap! output str s))}
          (nbb/load-string "
    (ns foo0 (:require [clojure.test :as t :refer [deftest is testing]]))
    (defmethod t/report [:cljs.test/default :end-run-tests] [m])
    (t/deftest foo (t/is (= 1 2))) (t/run-tests 'foo0)"))
        (.then (fn [_]
                 (is (str/includes? @output "expected: (= 1 2)"))
                 (is (str/includes? @output "actual: (not (= 1 2))")))))))

(deftest-async cljs-test-test
  (let [output (atom "")]
    (-> (with-async-bindings
          {sci/print-fn (fn [s]
                          (swap! output str s))}
          (nbb/load-string "
    (ns foo01 (:require [cljs.test :as t :refer [deftest is testing]]))
    (defmethod t/report [:cljs.test/default :end-run-tests] [m])
    (t/deftest foo (t/is (= 1 2)))
    (cljs.test/deftest bar (t/is (= 1 2))) (t/run-tests 'foo01)"))
        (.then (fn [_]
                 (is (str/includes? @output "expected: (= 1 2)"))
                 (is (str/includes? @output "actual: (not (= 1 2))")))))))

(deftest-async refer-macros-test
  (let [output (atom "")]
    (-> (with-async-bindings
          {sci/print-fn (fn [s]
                          (swap! output str s))}
          (nbb/load-string "
    (ns foo02 (:require [cljs.test :as t :refer-macros [deftest is]]))
    (defmethod t/report [:cljs.test/default :end-run-tests] [m])
    (deftest foo (is (= 1 2))) (t/run-tests 'foo02)"))
        (.then (fn [_]
                 (is (str/includes? @output "expected: (= 1 2)"))
                 (is (str/includes? @output "actual: (not (= 1 2))")))))))

(deftest-async are-test
  (let [output (atom "")]
    (with-async-bindings
      {sci/print-fn (fn [s]
                      (swap! output str s))}
      (->
       (nbb/load-string "
    (ns foo-are (:require [clojure.test :as t :refer [deftest is are testing]]))
    (defmethod t/report [:cljs.test/default :end-run-tests] [m])
    (t/deftest foo (t/are [x] (= x 2) 1 2 3)) (t/run-tests 'foo-are)")
       (.then (fn [_]
                (is (str/includes? @output "expected: (= 3 2)"))
                (is (str/includes? @output "actual: (not (= 3 2))"))))
       (.then (fn [_]
                (reset! output "")
                (nbb/load-string "
    (ns foo-are2 (:require [clojure.test :as t :refer [deftest is are testing]]))
    (deftest are-test
    (are [x y] (= x y)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)
       2 (inc 1)))
    (t/run-tests 'foo-are2)")))
       (.then (fn [_]
                (is (str/includes? @output " 9 assertions"))))))))

(deftest async-test-test
  (async done
         (let [output (atom "")
               old-printer @sci/print-fn
               old-newline @sci/print-newline
               restore (fn []
                         (sci/alter-var-root sci/print-fn (constantly
                                                           old-printer))
                         (sci/alter-var-root sci/print-newline (constantly old-newline)))]
           (sci/alter-var-root sci/print-fn (constantly
                                             (fn [s]
                                               (swap! output str s))))
           (sci/alter-var-root sci/print-newline (constantly true))
           (nbb/load-string "
    (ns foo1 (:require [clojure.test :as t :refer [deftest async is testing]]))
    (defmethod t/report [:cljs.test/default :end-run-tests] [m])
    (deftest foo (async done
                   (js/setTimeout #(do (t/is (= 1 2)) (done)) 300)))
    (t/run-tests 'foo1)")
           (let [f (fn f [t]
                     (js/setTimeout (fn []
                                      (cond (and (str/includes? @output "expected: (= 1 2)")
                                                 (str/includes? @output "actual: (not (= 1 2))"))
                                            (do (is (= :expected-output :expected-output))
                                                (restore)
                                                (done))
                                            (> t 500) (do (is (= :timeout t))
                                                          (restore)
                                                          (done))
                                            :else (f (+ t 10)))) 10))]
             (f 0)))))

(deftest end-run-tests-test
  (async done
         (-> (with-async-bindings
               {sci/print-fn (fn [_])}
               (nbb/load-string "
    (ns foo2 (:require [clojure.test :as t :refer [deftest async is testing]]))
    (deftest foo (async done
                   (js/setTimeout #(do (t/is (= 1 2)) (done)) 300)))

    (def resolver (atom nil))

    (defmethod t/report [:cljs.test/default :end-run-tests] [m]
      (@resolver m))

    (t/run-tests 'foo2)

    (js/Promise. (fn [resolve] (reset! resolver resolve)))
"))
             (.then (fn [m]
                      (is (= {:test 1, :pass 0, :fail 1, :error 0, :type :end-run-tests} m))
                      (done))))))

(deftest use-fixtures-test
  (async done
         (-> (with-async-bindings
               {sci/print-fn (fn [_])}
               (nbb/load-string "
(ns foo3 (:require [clojure.test :as t :refer [deftest test-vars async is testing use-fixtures]]))

(def state (atom []))

(use-fixtures :once
    {:before
     (fn []
       (swap! state conj :once-before))
     :after
     (fn []
       (swap! state conj :once-after))})

(use-fixtures :each
    {:before
     (fn []
       (swap! state conj :each-before))
     :after
     (fn []
       (swap! state conj :each-after))})

(deftest foo)
(deftest bar)

(test-vars [#'foo3/foo #'foo3/bar])

(swap! state conj :after-test-vars)

(t/run-tests 'foo3)

@state
"))
             (.then
              (fn [m]
                (is (= [:once-before :each-before :each-after :each-before :each-after :once-after
                        :after-test-vars
                        :once-before :each-before :each-after :each-before :each-after :once-after] m))
                (done))))))

(deftest report-binding-test
  (async done
         (-> (with-async-bindings
               {sci/print-fn (fn [_])}
               (nbb/load-string "
(ns foo4
  (:require [clojure.test :as t :refer [report]]))

(defmethod t/report [:cljs.test/default :end-run-tests] [m])

(def state (atom []))

(defmulti mine :type)

(defmethod mine :summary [_]
  (swap! state conj :summary))

(defmethod mine :default [_]
  (swap! state conj :default))

(defmethod mine :end-run-tests [_]
  (swap! state conj :end-run-tests))

(t/deftest hello
  (t/is (= 1 2)))

(binding [report mine]
  (t/run-tests 'foo4))

@state
"))
             (.then
              (fn [m]
                (is (= [:default :default :default :default :default :summary :end-run-tests] m))
                (done))))))

(deftest issue-234-assert-expr
  (async done
         (-> (with-async-bindings
               {sci/print-fn (fn [_])}
               (nbb/load-string
                "(require '[cljs.test :as t]) (defmethod t/assert-expr 'foo [menv msg form] :hello) (t/is (foo))"))
             (.then
              (fn [m]
                (is (= :hello m))
                (done))))))

(deftest-async testing-test
  (let [output (atom "")]
    (-> (with-async-bindings
          {sci/print-fn (fn [s]
                          (swap! output str s))}
          (nbb/load-string "
    (ns foo0 (:require [clojure.test :as t :refer [deftest is testing]]))
    (t/deftest foo (testing \"Testing macro test\" (t/is (= 1 2)))) (t/run-tests 'foo0)"))
        (.then (fn [_]
                 (is (str/includes? @output "Testing macro test")))))))
