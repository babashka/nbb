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
    (t/deftest foo (t/is (= 1 2))) (t/run-tests 'foo0)"))
        (.then (fn [_]
                 (is (str/includes? @output "expected: (= 1 2)  actual: (not (= 1 2))")))))))

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
