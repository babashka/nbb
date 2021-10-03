(ns nbb.impl.test
  (:require
   [nbb.core :as nbb :refer [sci-ctx]]
   [nbb.impl.clojure.test :as t]
   [sci.core :as sci]))

(def tns t/tns)

(defn new-var [var-sym f]
  (sci/new-var var-sym f {:ns tns}))

(def clojure-test-namespace
  {:obj tns
   'async (sci/copy-var t/async tns)
   '-async-test (sci/copy-var t/-async-test tns)
   '*load-tests* t/load-tests
   '*stack-trace-depth* t/stack-trace-depth
   '*report-counters* t/report-counters
   '*initial-report-counters* t/initial-report-counters
   '*testing-vars* t/testing-vars
   '*testing-contexts* t/testing-contexts
   ;; 'with-test-out (macrofy @#'t/with-test-out)
   ;; 'file-position t/file-position
   'testing-vars-str (sci/copy-var t/testing-vars-str tns)
   'testing-contexts-str (sci/copy-var t/testing-contexts-str tns)
   'inc-report-counter! (sci/copy-var t/inc-report-counter! tns)
   'report t/report
   'do-report (sci/copy-var t/do-report tns)
   ;; assertion utilities
   'function? (sci/copy-var t/function? tns)
   'assert-predicate (sci/copy-var t/assert-predicate tns)
   'assert-any (sci/copy-var t/assert-any tns)
   ;; assertion methods
   'assert-expr (sci/copy-var t/assert-expr tns)
   'try-expr (sci/copy-var t/try-expr tns)
   ;; assertion macros
   'is (sci/copy-var t/is tns)
   'are (sci/copy-var t/are tns)
   'testing (sci/copy-var t/testing tns)
   ;; defining tests
   'with-test (sci/copy-var t/with-test tns)
   'deftest (sci/copy-var t/deftest tns)
   'deftest- (sci/copy-var t/deftest- tns)
   'set-test (sci/copy-var t/set-test tns)
   ;; fixtures
   'use-fixtures (sci/copy-var t/use-fixtures tns)
   'compose-fixtures (sci/copy-var t/compose-fixtures tns)
   'join-fixtures (sci/copy-var t/join-fixtures tns)
   ;; running tests: low level
   'test-var t/test-var
   ;; TODO:
   ;; 'test-vars (sci/copy-var t/test-vars tns)
   ;; TODO:
   ;;'test-all-vars (new-var 'test-all-vars (contextualize t/test-all-vars))
   ;; TODO:
   ;;'test-ns (new-var 'test-ns (contextualize t/test-ns))
   ;; running tests: high level
   'run-tests (sci/copy-var t/run-tests tns)
   ;; 'run-all-tests (sci/copy-var t/run-all-tests tns)
   'successful? (sci/copy-var t/successful? tns)})

(defn init []
  (nbb/register-plugin!
   ::reagent-dom-server
   {:namespaces {'clojure.test clojure-test-namespace}}))
