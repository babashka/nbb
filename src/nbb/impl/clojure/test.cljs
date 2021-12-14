;;   Copyright (c) Rich Hickey. All rights reserved.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

;;; test.clj: test framework for Clojure

;; by Stuart Sierra
;; March 28, 2009

;; Thanks to Chas Emerick, Allen Rohner, and Stuart Halloway for
;; contributions and suggestions.

(ns
    ^{:author "Stuart Sierra, with contributions and suggestions by
  Chas Emerick, Allen Rohner, and Stuart Halloway",
      :doc "A unit testing framework.

   ASSERTIONS

   The core of the library is the \"is\" macro, which lets you make
   assertions of any arbitrary expression:

   (is (= 4 (+ 2 2)))
   (is (instance? Integer 256))
   (is (.startsWith \"abcde\" \"ab\"))

   You can type an \"is\" expression directly at the REPL, which will
   print a message if it fails.

       user> (is (= 5 (+ 2 2)))

       FAIL in  (:1)
       expected: (= 5 (+ 2 2))
         actual: (not (= 5 4))
       false

   The \"expected:\" line shows you the original expression, and the
   \"actual:\" shows you what actually happened.  In this case, it
   shows that (+ 2 2) returned 4, which is not = to 5.  Finally, the
   \"false\" on the last line is the value returned from the
   expression.  The \"is\" macro always returns the result of the
   inner expression.

   There are two special assertions for testing exceptions.  The
   \"(is (thrown? c ...))\" form tests if an exception of class c is
   thrown:

   (is (thrown? ArithmeticException (/ 1 0)))

   \"(is (thrown-with-msg? c re ...))\" does the same thing and also
   tests that the message on the exception matches the regular
   expression re:

   (is (thrown-with-msg? ArithmeticException #\"Divide by zero\"
                         (/ 1 0)))

   DOCUMENTING TESTS

   \"is\" takes an optional second argument, a string describing the
   assertion.  This message will be included in the error report.

   (is (= 5 (+ 2 2)) \"Crazy arithmetic\")

   In addition, you can document groups of assertions with the
   \"testing\" macro, which takes a string followed by any number of
   assertions.  The string will be included in failure reports.
   Calls to \"testing\" may be nested, and all of the strings will be
   joined together with spaces in the final report, in a style
   similar to RSpec <http://rspec.info/>

   (testing \"Arithmetic\"
     (testing \"with positive integers\"
       (is (= 4 (+ 2 2)))
       (is (= 7 (+ 3 4))))
     (testing \"with negative integers\"
       (is (= -4 (+ -2 -2)))
       (is (= -1 (+ 3 -4)))))

   Note that, unlike RSpec, the \"testing\" macro may only be used
   INSIDE a \"deftest\" or \"with-test\" form (see below).


   DEFINING TESTS

   There are two ways to define tests.  The \"with-test\" macro takes
   a defn or def form as its first argument, followed by any number
   of assertions.  The tests will be stored as metadata on the
   definition.

   (with-test
       (defn my-function [x y]
         (+ x y))
     (is (= 4 (my-function 2 2)))
     (is (= 7 (my-function 3 4))))

   As of Clojure SVN rev. 1221, this does not work with defmacro.
   See http://code.google.com/p/clojure/issues/detail?id=51

   The other way lets you define tests separately from the rest of
   your code, even in a different namespace:

   (deftest addition
     (is (= 4 (+ 2 2)))
     (is (= 7 (+ 3 4))))

   (deftest subtraction
     (is (= 1 (- 4 3)))
     (is (= 3 (- 7 4))))

   This creates functions named \"addition\" and \"subtraction\", which
   can be called like any other function.  Therefore, tests can be
   grouped and composed, in a style similar to the test framework in
   Peter Seibel's \"Practical Common Lisp\"
   <http://www.gigamonkeys.com/book/practical-building-a-unit-test-framework.html>

   (deftest arithmetic
     (addition)
     (subtraction))

   The names of the nested tests will be joined in a list, like
   \"(arithmetic addition)\", in failure reports.  You can use nested
   tests to set up a context shared by several tests.


   RUNNING TESTS

   Run tests with the function \"(run-tests namespaces...)\":

   (run-tests 'your.namespace 'some.other.namespace)

   If you don't specify any namespaces, the current namespace is
   used.  To run all tests in all namespaces, use \"(run-all-tests)\".

   By default, these functions will search for all tests defined in
   a namespace and run them in an undefined order.  However, if you
   are composing tests, as in the \"arithmetic\" example above, you
   probably do not want the \"addition\" and \"subtraction\" tests run
   separately.  In that case, you must define a special function
   named \"test-ns-hook\" that runs your tests in the correct order:

   (defn test-ns-hook []
     (arithmetic))

   Note: test-ns-hook prevents execution of fixtures (see below).


   OMITTING TESTS FROM PRODUCTION CODE

   You can bind the variable \"*load-tests*\" to false when loading or
   compiling code in production.  This will prevent any tests from
   being created by \"with-test\" or \"deftest\".


   FIXTURES

   Fixtures allow you to run code before and after tests, to set up
   the context in which tests should be run.

   A fixture is just a function that calls another function passed as
   an argument.  It looks like this:

   (defn my-fixture [f]
      Perform setup, establish bindings, whatever.
     (f)  Then call the function we were passed.
      Tear-down / clean-up code here.
    )

   Fixtures are attached to namespaces in one of two ways.  \"each\"
   fixtures are run repeatedly, once for each test function created
   with \"deftest\" or \"with-test\".  \"each\" fixtures are useful for
   establishing a consistent before/after state for each test, like
   clearing out database tables.

   \"each\" fixtures can be attached to the current namespace like this:
   (use-fixtures :each fixture1 fixture2 ...)
   The fixture1, fixture2 are just functions like the example above.
   They can also be anonymous functions, like this:
   (use-fixtures :each (fn [f] setup... (f) cleanup...))

   The other kind of fixture, a \"once\" fixture, is only run once,
   around ALL the tests in the namespace.  \"once\" fixtures are useful
   for tasks that only need to be performed once, like establishing
   database connections, or for time-consuming tasks.

   Attach \"once\" fixtures to the current namespace like this:
   (use-fixtures :once fixture1 fixture2 ...)

   Note: Fixtures and test-ns-hook are mutually incompatible.  If you
   are using test-ns-hook, fixture functions will *never* be run.


   SAVING TEST OUTPUT TO A FILE

   All the test reporting functions write to the var *test-out*.  By
   default, this is the same as *out*, but you can rebind it to any
   PrintWriter.  For example, it could be a file opened with
   clojure.java.io/writer.


   EXTENDING TEST-IS (ADVANCED)

   You can extend the behavior of the \"is\" macro by defining new
   methods for the \"assert-expr\" multimethod.  These methods are
   called during expansion of the \"is\" macro, so they should return
   quoted forms to be evaluated.

   You can plug in your own test-reporting framework by rebinding
   the \"report\" function: (report event)

   The 'event' argument is a map.  It will always have a :type key,
   whose value will be a keyword signaling the type of event being
   reported.  Standard events with :type value of :pass, :fail, and
   :error are called when an assertion passes, fails, and throws an
   exception, respectively.  In that case, the event will also have
   the following keys:

     :expected   The form that was expected to be true
     :actual     A form representing what actually occurred
     :message    The string message given as an argument to 'is'

   The \"testing\" strings will be a list in \"*testing-contexts*\", and
   the vars being tested will be a list in \"*testing-vars*\".

   Your \"report\" function should wrap any printing calls in the
   \"with-test-out\" macro, which rebinds *out* to the current value
   of *test-out*.

   For additional event types, see the examples in the code.
"}
    nbb.impl.clojure.test
  (:refer-clojure :exclude [println])
  (:require #_[clojure.template :as temp]
            [clojure.string :as str]
            [nbb.core :refer [sci-ctx]]
            [sci.core :as sci]
            [sci.impl.io :refer [println]]
            [sci.impl.namespaces :as sci-namespaces]
            [sci.impl.resolve :as resolve]
            [sci.impl.vars :as vars])
  (:require-macros [nbb.impl.clojure.test :refer [with-test-out-internal]]))

;; TODO: go through https://github.com/clojure/clojurescript/blob/r1.10.879-6-gaec9f0c5/src/main/cljs/cljs/test.cljc for compatibility
;; and https://github.com/clojure/clojurescript/blob/r1.10.879-6-gaec9f0c5/src/main/cljs/cljs/test.cljs

(def ctx sci-ctx)

;; Nothing is marked "private" here, so you can rebind things to plug
;; in your own testing or reporting frameworks.

(def tns (sci/create-ns 'clojure.test nil))

;;; USER-MODIFIABLE GLOBALS

(defonce
  ^{:doc "True by default.  If set to false, no test functions will
   be created by deftest, set-test, or with-test.  Use this to omit
   tests when compiling or loading production code."}
  load-tests
  (sci/new-dynamic-var '*load-tests* true {:ns tns}))

(def
  ^{:doc "The maximum depth of stack traces to print when an Exception
  is thrown during a test.  Defaults to nil, which means print the
  complete stack trace."}
  stack-trace-depth
  (sci/new-dynamic-var '*stack-trace-depth* nil {:ns tns}))


;;; GLOBALS USED BY THE REPORTING FUNCTIONS

(def report-counters (sci/new-dynamic-var '*report-counters* nil {:ns tns}))     ; bound to a ref of a map in test-ns

(def initial-report-counters  ; used to initialize *report-counters*
  (sci/new-dynamic-var '*initial-report-counters* {:test 0, :pass 0, :fail 0, :error 0} {:ns tns}))

(def testing-vars (sci/new-dynamic-var '*testing-vars* (list) {:ns tns}))  ; bound to hierarchy of vars being tested

(def testing-contexts (sci/new-dynamic-var '*testing-contexts* (list) {:ns tns})) ; bound to hierarchy of "testing" strings

;;; UTILITIES FOR REPORTING FUNCTIONS

;; =============================================================================
;; Default Reporting

(defn empty-env
  "Generates a testing environment with a reporter.
   (empty-env) - uses the :cljs.test/default reporter.
   (empty-env :cljs.test/pprint) - pretty prints all data structures.
   (empty-env reporter) - uses a reporter of your choosing.
   To create your own reporter see cljs.test/report"
  ([] (empty-env :cljs.test/default))
  ([reporter]
   (cond-> {:report-counters {:test 0 :pass 0 :fail 0 :error 0}
            :testing-vars ()
            :testing-contexts ()
            :formatter pr-str
            :reporter reporter}
     (= :cljs.test/pprint reporter) (assoc :reporter :cljs.test/default
                                  :formatter prn #_pprint/pprint))))

(def ^:dynamic *current-env* nil)

(defn get-current-env []
  (or *current-env* (empty-env)))

(defn update-current-env! [ks f & args]
  (set! *current-env* (apply update-in (get-current-env) ks f args)))

(defn set-env! [new-env]
  (set! *current-env* new-env))

(defn clear-env! []
  (set! *current-env* nil))

(defn get-and-clear-env!
  "Like get-current-env, but cleans env before returning."
  []
  (let [env (get-current-env)]
    (clear-env!)
    env))

(defn testing-vars-str
  "Returns a string representation of the current test.  Renders names
  in *testing-vars* as a list, then the source file and line of
  current assertion."
  [m]
  (let [{:keys [file line column]} m]
    (str
     (reverse (map #(:name (meta %)) (:testing-vars (get-current-env))))
     " (" file ":" line (when column (str ":" column)) ")")))

(defn testing-contexts-str
  "Returns a string representation of the current test context. Joins
  strings in *testing-contexts* with spaces."
  []
  (apply str (interpose " " (reverse (:testing-contexts (get-current-env))))))

(defn inc-report-counter!
  "Increments the named counter in *report-counters*, a ref to a map.
  Does nothing if *report-counters* is nil."
  [name]
  (when (:report-counters (get-current-env))
    (update-current-env! [:report-counters name] (fnil inc 0))))

(defmulti
  ^{:doc "Generic reporting function, may be overridden to plug in
   different report formats (e.g., TAP, JUnit).  Assertions such as
   'is' call 'report' to indicate results.  The argument given to
   'report' will be a map with a :type key."
    :dynamic true}
  report-impl (fn [m]
                [(:reporter (get-current-env)) (:type m)]))

(def report (sci/copy-var report-impl tns))

(defmethod report-impl [:cljs.test/default :pass] [_m]
  (inc-report-counter! :pass))

(defn- print-comparison [m]
  (let [formatter-fn (or (:formatter (get-current-env)) pr-str)]
    (println "expected:" (formatter-fn (:expected m)))
    (println "  actual:" (formatter-fn (:actual m)))))

(defmethod report-impl [:cljs.test/default :fail] [m]
  (inc-report-counter! :fail)
  (println "\nFAIL in" (testing-vars-str m))
  (when (seq (:testing-contexts (get-current-env)))
    (println (testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (print-comparison m))

(defmethod report-impl [:cljs.test/default :error] [m]
  (inc-report-counter! :error)
  (println "\nERROR in" (testing-vars-str m))
  (when (seq (:testing-contexts (get-current-env)))
    (println (testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (print-comparison m))

(defmethod report-impl [:cljs.test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
           (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors."))

(defmethod report-impl [:cljs.test/default :begin-test-ns] [m]
  (println "\nTesting" (:ns m)))

;; Ignore these message types:
(defmethod report-impl [:cljs.test/default :end-test-ns] [m])
(defmethod report-impl [:cljs.test/default :begin-test-var] [m]
  #_(println ":begin-test-var" (testing-vars-str m)))
(defmethod report-impl [:cljs.test/default :end-test-var] [m])
(defmethod report-impl [:cljs.test/default :end-run-tests] [m])
(defmethod report-impl [:cljs.test/default :end-test-all-vars] [m])
(defmethod report-impl [:cljs.test/default :end-test-vars] [m])

;;; TEST RESULT REPORTING

(defn js-filename [stack-element]
  #_(let [output-dir "out"
        output-dir (cond-> output-dir
                     (not (str/ends-with? output-dir "/"))
                     (str "/"))]
    (-> (.split stack-element output-dir)
        last
        (.split ":")
        first)))

(defn mapped-line-and-column [filename line column]
  (let [default [filename line column]]
    (if-let [source-map (:source-map (get-current-env))]
      ;; source maps are 0 indexed for lines
      (if-let [columns (get-in source-map [filename (dec line)])]
        (vec
         (map
          ;; source maps are 0 indexed for columns
          ;; multiple segments may exist at column
          ;; just take first
          (first
           (if-let [mapping (get columns (dec column))]
             mapping
             (second (first columns))))
          [:source :line :col]))
        default)
      default)))

(defn file-and-line [exception depth]
  ;; TODO: flesh out
  (if-let [stack-element (and (string? (.-stack exception))
                              (some-> (.-stack exception)
                                      str/split-lines
                                      (get depth)
                                      str/trim))]
    (let [fname "todo" #_(js-filename stack-element)
          [line column] nil #_(js-line-and-column stack-element)
          [fname line column] (mapped-line-and-column fname line column)]
      {:file fname :line line :column column})
    {:file (.-fileName exception)
     :line (.-lineNumber exception)}))

(defn do-report [m]
  (let [m (case (:type m)
            :fail (merge (file-and-line (js/Error.) 4) m)
            :error (merge (file-and-line (:actual m) 0) m)
            m)]
    (report m)))

#_(defn do-report
  "Add file and line information to a test result and call report.
   If you are writing a custom assert-expr method, call this function
   to pass test results to report."
  {:added "1.2"}
  [m]
  (report-impl
   (case
       (:type m)
     :fail m
     :error m
     m)))

;;; UTILITIES FOR ASSERTIONS

(defn get-possibly-unbound-var
  "Like var-get but returns nil if the var is unbound."
  {:added "1.1"}
  [v]
  (try (deref v)
       (catch :default _
         nil)))

(defn function?
  "Returns true if argument is a function or a symbol that resolves to
  a function (not a macro)."
  {:added "1.1"}
  [x]
  (if (symbol? x)
    (when-let [v (second (resolve/lookup @ctx x false))]
      (when-let [value (if (vars/var? v)
                         (get-possibly-unbound-var v)
                         v)]
        (and (fn? value)
             (not (:macro (meta v)))
             (not (:sci/macro (meta v))))))
    (fn? x)))

(defn assert-predicate
  "Returns generic assertion code for any functional predicate.  The
  'expected' argument to 'report' will contains the original form, the
  'actual' argument will contain the form with all its sub-forms
  evaluated.  If the predicate returns false, the 'actual' form will
  be wrapped in (not...)."
  {:added "1.1"}
  [msg form]
  (let [args (rest form)
        pred (first form)]
    `(let [values# (list ~@args)
           result# (apply ~pred values#)]
       (if result#
         (clojure.test/do-report {:type :pass, :message ~msg,
                                  :expected '~form, :actual (cons ~pred values#)})
         (clojure.test/do-report {:type :fail, :message ~msg,
                                  :file clojure.core/*file*
                                  :line ~(:line (meta form))
                                  :expected '~form, :actual (list '~'not (cons '~pred values#))}))
       result#)))

(defn assert-any
  "Returns generic assertion code for any test, including macros, Java
  method calls, or isolated symbols."
  {:added "1.1"}
  [msg form]
  `(let [value# ~form]
     (if value#
       (clojure.test/do-report {:type :pass, :message ~msg,
                                :expected '~form, :actual value#})
       (clojure.test/do-report {:type :fail, :message ~msg,
                                :file clojure.core/*file*
                                :line ~(:line (meta form))
                                :expected '~form, :actual value#}))
     value#))



;;; ASSERTION METHODS

;; You don't call these, but you can add methods to extend the 'is'
;; macro.  These define different kinds of tests, based on the first
;; symbol in the test expression.

(defmulti assert-expr 
  (fn [_menv _msg form]
    (cond
      (nil? form) :always-fail
      (seq? form) (first form)
      :else :default)))

(defmethod assert-expr :always-fail [_menv msg form]
  ;; nil test: always fail
  (let [{:keys [file line end-line column end-column]} (meta form)]
    `(clojure.test/report {:type :fail, :message ~msg
                           :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column})))

(defmethod assert-expr :default [_menv msg form]
  (if (and (sequential? form)
           (function? #_menv (first form)))
    (assert-predicate msg form)
    (assert-any msg form)))

(defmethod assert-expr 'instance? [_menv msg form]
  ;; Test if x is an instance of y.
  (let [{:keys [file line end-line column end-column]} (meta form)]
    `(let [klass# ~(nth form 1)
           object# ~(nth form 2)]
       (let [result# (instance? klass# object#)]
         (if result#
           (clojure.test/report
            {:type :pass, :message ~msg,
             :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
             :expected '~form, :actual (type object#)})
           (clojure.test/report
            {:type :fail, :message ~msg,
             :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
             :expected '~form, :actual (type object#)}))
         result#))))

(defmethod assert-expr 'thrown? [_menv msg form]
  ;; (is (thrown? c expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Returns the exception thrown.
  (let [{:keys [file line end-line column end-column]} (meta form)
        klass (second form)
        body (nthnext form 2)]
    `(try
       ~@body
       (clojure.test/report
        {:type :fail, :message ~msg,
         :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
         :expected '~form, :actual nil})
       (catch ~klass e#
         (clojure.test/report
          {:type :pass, :message ~msg,
           :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
           :expected '~form, :actual e#})
         e#))))

#_(defmethod assert-expr 'thrown? [msg form]
  ;; (is (thrown? c expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Returns the exception thrown.
  (let [klass (second form)
        body (nthnext form 2)]
    `(try ~@body
          (clojure.test/do-report {:type :fail, :message ~msg,
                                   :file clojure.core/*file*
                                   :line ~(:line (meta form))
                                   :expected '~form, :actual nil})
          (catch ~klass e#
            (clojure.test/do-report {:type :pass, :message ~msg,
                                     :expected '~form, :actual e#})
            e#))))

(defmethod assert-expr 'thrown-with-msg? [menv msg form]
  ;; (is (thrown-with-msg? c re expr))
  ;; Asserts that evaluating expr throws an exception of class c.
  ;; Also asserts that the message string of the exception matches
  ;; (with re-find) the regular expression re.
  (let [{:keys [file line end-line column end-column]} (meta form)
        klass (nth form 1)
        re (nth form 2)
        body (nthnext form 3)]
    `(try
       ~@body
       (clojure.test/report {:type :fail, :message ~msg, :expected '~form, :actual nil
                :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column})
       (catch ~klass e#
         (let [m# (.-message e#)]
           (if (re-find ~re m#)
             (clojure.test/report
              {:type :pass, :message ~msg,
               :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
               :expected '~form, :actual e#})
             (clojure.test/report
              {:type :fail, :message ~msg,
               :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
               :expected '~form, :actual e#}))
           e#)))))

(defn ^:macro try-expr
  "Used by the 'is' macro to catch unexpected exceptions.
  You don't call this."
  [_ &env msg form]
  (let [{:keys [file line end-line column end-column]} (meta form)]
    `(try
       ~(assert-expr &env msg form)
       (catch :default t#
         (clojure.test/report
          {:type :error, :message ~msg,
           :file ~file :line ~line :end-line ~end-line :column ~column :end-column ~end-column
           :expected '~form, :actual t#})))))


#_(defn ^:macro try-expr
  "Used by the 'is' macro to catch unexpected exceptions.
  You don't call this."
  {:added "1.1"}
  [_ _ msg form]
  `(try ~(assert-expr msg form)
        (catch :default t#
          (clojure.test/do-report {:file clojure.core/*file*
                                   :line ~(:line (meta form))
                                   :type :error, :message ~msg,
                                   :expected '~form, :actual t#}))))



;;; ASSERTION MACROS

;; You use these in your tests.

(defn ^:macro is
  "Generic assertion macro.  'form' is any predicate test.
  'msg' is an optional message to attach to the assertion.

  Example: (is (= 4 (+ 2 2)) \"Two plus two should be 4\")

  Special forms:

  (is (thrown? c body)) checks that an instance of c is thrown from
  body, fails if not; then returns the thing thrown.

  (is (thrown-with-msg? c re body)) checks that an instance of c is
  thrown AND that the message on the exception matches (with
  re-find) the regular expression re."
  {:added "1.1"}
  ([_ _ form]
   `(clojure.test/is ~form nil))
  ([_ _ form msg]
   `(clojure.test/try-expr ~msg ~form)))

(defn ^:macro are
  "Checks multiple assertions with a template expression.
  See clojure.template/do-template for an explanation of
  templates.

  Example: (are [x y] (= x y)
                2 (+ 1 1)
                4 (* 2 2))
  Expands to:
           (do (is (= 2 (+ 1 1)))
               (is (= 4 (* 2 2))))

  Note: This breaks some reporting features, such as line numbers."
  {:added "1.1"}
  [_ _ argv expr & args]
  (if (or
       ;; (are [] true) is meaningless but ok
       (and (empty? argv) (empty? args))
       ;; Catch wrong number of args
       (and (pos? (count argv))
            (pos? (count args))
            (zero? (mod (count args) (count argv)))))
    `(clojure.template/do-template ~argv (clojure.test/is ~expr) ~@args)
    (throw (js/Error. "The number of args doesn't match are's argv."))))

(defn ^:macro testing
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  {:added "1.1"}
  [_ _ string & body]
  `(binding [clojure.test/*testing-contexts* (conj clojure.test/*testing-contexts* ~string)]
     ~@body))



;;; DEFINING TESTS

(defn ^:macro with-test
  "Takes any definition form (that returns a Var) as the first argument.
  Remaining body goes in the :test metadata function for that Var.

  When *load-tests* is false, only evaluates the definition, ignoring
  the tests."
  {:added "1.1"}
  [_ _ definition & body]
  (if @load-tests
    `(doto ~definition (alter-meta! assoc :test (fn [] ~@body)))
    definition))


(defn ^:macro deftest
  "Defines a test function with no arguments.  Test functions may call
  other tests, so tests may be composed.  If you compose tests, you
  should also define a function named test-ns-hook; run-tests will
  call test-ns-hook instead of testing all vars.

  Note: Actually, the test body goes in the :test metadata on the var,
  and the real function (the value of the var) calls test-var on
  itself.

  When *load-tests* is false, deftest is ignored."
  {:added "1.1"}
  [_ _ name & body]
  (when @load-tests
    `(def ~(vary-meta name assoc :test `(fn [] ~@body))
       (fn [] (clojure.test/test-var (var ~name))))))

(defn ^:macro deftest-
  "Like deftest but creates a private var."
  {:added "1.1"}
  [_ _ name & body]
  (when @load-tests
    `(def ~(vary-meta name assoc :test `(fn [] ~@body) :private true)
       (fn [] (test-var (var ~name))))))


(defn ^:macro set-test
  "Experimental.
  Sets :test metadata of the named var to a fn with the given body.
  The var must already exist.  Does not modify the value of the var.

  When *load-tests* is false, set-test is ignored."
  {:added "1.1"}
  [_ _ name & body]
  (when @load-tests
    `(alter-meta! (var ~name) assoc :test (fn [] ~@body))))



;;; DEFINING FIXTURES


(defn- default-fixture
  "The default, empty, fixture function.  Just calls its argument."
  {:added "1.1"}
  [f]
  (f))

(defn compose-fixtures
  "Composes two fixture functions, creating a new fixture function
  that combines their behavior."
  {:added "1.1"}
  [f1 f2]
  (fn [g] (f1 (fn [] (f2 g)))))

(defn join-fixtures
  "Composes a collection of fixtures, in order.  Always returns a valid
  fixture function, even if the collection is empty."
  {:added "1.1"}
  [fixtures]
  (reduce compose-fixtures default-fixture fixtures))

(defn- execution-strategy [once each]
  (letfn [(fixtures-type [coll]
            (cond
              (empty? coll) :none
              (every? map? coll) :map
              (every? fn? coll) :fn))
          (fixtures-types []
            (->> (map fixtures-type [once each])
                 (remove #{:none})
                 (distinct)))]
    (let [[type :as types] (fixtures-types)]
      (assert (not-any? nil? types)
              "Fixtures may not be of mixed types")
      (assert (> 2 (count types))
              "fixtures specified in :once and :each must be of the same type")
      ({:map :async :fn :sync} type :async))))

(defn- wrap-map-fixtures
  "Wraps block in map-fixtures."
  [map-fixtures block]
  (concat (keep :before map-fixtures)
          block
          (reverse (keep :after map-fixtures))))

;; =============================================================================
;; Async

(defprotocol IAsyncTest
  "Marker protocol denoting CPS function to begin asynchronous
  testing.")

(defn async?
  "Returns whether x implements IAsyncTest."
  [x]
  (satisfies? IAsyncTest x))

(defn run-block
  "Invoke all functions in fns with no arguments. A fn can optionally
  return
  an async test - is invoked with a continuation running left fns
  a seq of fns tagged per block - are invoked immediately after fn"
  [fns]
  (when-first [f fns]
    (let [obj (f)]
      (if (async? obj)
        (obj (let [d (delay
                       (run-block (rest fns)))]
               (fn []
                 (if (realized? d)
                   (println "WARNING: Async test called done more than one time.")
                   @d))))
        (recur (cond->> (rest fns)
                 (:cljs.test/block? (meta obj)) (concat obj)))))))

(defn block
  "Tag a seq of fns to be picked up by run-block as injected
  continuation.  See run-block."
  [fns]
  (some-> fns
          (vary-meta assoc :cljs.test/block? true)))

(defn ^:macro async
  "Wraps body as a CPS function that can be returned from a test to
  continue asynchronously.  Binds done to a function that must be
  invoked once and from an async context after any assertions.
  (deftest example-with-timeout
    (async done
      (js/setTimeout (fn []
                       ;; make assertions in async context...
                       (done) ;; ...then call done
                       )
                     0)))"
  [_ _ done & body]
  `(clojure.test/-async-test
    (fn [_# ~done] ~@body)))

(defn -async-test
  [f]
  (reify
    IAsyncTest
    cljs.core/IFn
    (-invoke [_ x]
      (f _ x))))

;;; RUNNING TESTS: LOW-LEVEL FUNCTIONS

(defn- test-var-block*
  [v t]
  {:pre [(vars/var? v)]}
  [(fn []
     (update-current-env! [:testing-vars] conj v)
     (update-current-env! [:report-counters :test] inc)
     (do-report {:type :begin-test-var :var v})
     (try
       (t)
       (catch :default e
         (case e
           :cljs.test/async-disabled (throw "Async tests require fixtures to be specified as maps.  Testing aborted.")
           (do-report
            {:type :error
             :message "Uncaught exception, not in assertion."
             :expected nil
             :actual e})))))
   (fn []
     (do-report {:type :end-test-var :var v})
     (update-current-env! [:testing-vars] rest))])

(defn test-var-block
  "Like test-var, but returns a block for further composition and
  later execution."
  [v]
  (if-let [t (:test (meta v))]
    (test-var-block* v t)))

(defn test-var-impl
  "If v has a function in its :test metadata, calls that function,
  add v to :testing-vars property of env."
  [v]
  (run-block (test-var-block v)))

(def test-var (sci/copy-var test-var-impl tns))

(defn- disable-async [f]
  (fn []
    (let [obj (f)]
      (when (async? obj)
        (throw :cljs.test/async-disabled))
      obj)))

(defn test-vars-block
  "Like test-vars, but returns a block for further composition and
  later execution."
  [vars]
  (map
   (fn [[ns vars]]
     (fn []
       (let [ns (symbol (str ns))
             ni (sci-namespaces/sci-ns-interns @ctx ns)
             _ (when-let [fs (get ni 'cljs-test-once-fixtures)]
                 (update-current-env! [:once-fixtures] assoc ns
                                      @fs))
             _ (when-let [fs (get ni 'cljs-test-each-fixtures)]
                 (update-current-env! [:each-fixtures] assoc ns
                                      @fs))]
         (block
          (let [env (get-current-env)
                once-fixtures (get-in env [:once-fixtures (symbol (str ns))])
                each-fixtures (get-in env [:each-fixtures (symbol (str ns))])]
            (case (execution-strategy once-fixtures each-fixtures)
              :async
              (->> vars
                   (filter (comp :test meta))
                   (mapcat (comp (partial wrap-map-fixtures each-fixtures)
                                 test-var-block))
                   (wrap-map-fixtures once-fixtures))
              :sync
              (let [each-fixture-fn (join-fixtures each-fixtures)]
                [(fn []
                   ((join-fixtures once-fixtures)
                    (fn []
                      (doseq [v vars]
                        (when-let [t (:test (meta v))]
                          ;; (alter-meta! v update :test disable-async)
                          (each-fixture-fn
                           (fn []
                             ;; (test-var v)
                             (run-block
                              (test-var-block* v (disable-async t))))))))))])))))))
   (group-by (comp :ns meta) vars)))

(defn test-vars
  "Groups vars by their namespace and runs test-vars on them with
  appropriate fixtures assuming they are present in the current
  testing environment."
  [vars]
  (run-block (concat (test-vars-block vars)
                     [(fn []
                        (report {:type :end-test-vars :vars vars}))])))

(defn test-all-vars-block
  ([ns]
   (let [env (get-current-env)
         ni (sci-namespaces/sci-ns-interns @ctx ns)]
     (concat
      [(fn []
         (when (nil? env)
           (set-env! (empty-env)))
         (when-let [fs (get ni 'cljs-test-once-fixtures)]
           (update-current-env! [:once-fixtures] assoc ns
                                @fs))
         (when-let [fs (get ni 'cljs-test-each-fixtures)]
           (update-current-env! [:each-fixtures] assoc ns
                                @fs)))]
      (test-vars-block
       (let [vars (vals (sci-namespaces/sci-ns-interns @ctx ns))
             tests (filter (fn [var] (:test (meta var))) vars)
             sorted (sort-by (fn [var] (:line (meta var))) tests)]
         sorted))
      [(fn []
         (when (nil? env)
           (clear-env!)))]))))

(defn test-ns-block
  "Like test-ns, but returns a block for further composition and
  later execution.  Does not clear the current env."
  ([env form]
   #_(assert (and (= quote 'quote) (symbol? ns)) "Argument to test-ns must be a quoted symbol")
   #_(assert (ana-api/find-ns ns) (str "Namespace " ns " does not exist"))
   [(fn []
      (set-env! env)
      (do-report {:type :begin-test-ns, :ns form})
      ;; If the namespace has a test-ns-hook function, call that:
      (if-let [v  false #_(ana-api/ns-resolve ns 'test-ns-hook)]
        nil #_`(~(symbol (name ns) "test-ns-hook"))
        ;; Otherwise, just test every var in the namespace.
        (block (test-all-vars-block form))))
    (fn []
      (do-report {:type :end-test-ns, :ns form}))]))

;;; RUNNING TESTS: HIGH-LEVEL FUNCTIONS

(defn run-tests-block
  "Like test-vars, but returns a block for further composition and
  later execution."
  [env-or-ns & namespaces]
  #_(assert (every?
             (fn [[quote ns]] (and (= quote 'quote) (symbol? ns)))
             namespaces)
            "All arguments to run-tests must be quoted symbols")
  (let [is-ns (not (map? env-or-ns))
        env (if is-ns
              (empty-env)
              env-or-ns)
        summary (cljs.core/volatile!
                 {:test 0 :pass 0 :fail 0 :error 0
                  :type :summary})]
    (concat (mapcat
             (fn [ns]
               (concat (test-ns-block env ns)
                       [(fn []
                          (cljs.core/vswap!
                           summary
                           (partial merge-with +)
                           (:report-counters
                            (get-and-clear-env!))))]))
             (if is-ns
               (concat [env-or-ns] namespaces)
               namespaces))
            [(fn []
               (set-env! env)
               (do-report (deref summary))
               (report (assoc (deref summary) :type :end-run-tests))
               (clear-env!))])))

(defn run-tests
  "Runs all tests in the given namespaces; prints results.
  Defaults to current namespace if none given. Does not return a meaningful
  value due to the possiblity of asynchronous execution. To detect test
  completion add a :end-run-tests method case to the cljs.test/report
  multimethod."
  ([] (run-tests (empty-env) (vars/getName @sci/ns)))
  ([env-or-ns]
   (if (map? env-or-ns)
     (run-tests env-or-ns (vars/getName @sci/ns))
     (run-tests (empty-env) env-or-ns)))
  ([env-or-ns & namespaces]
   (run-block (apply run-tests-block env-or-ns namespaces))))

(defn ^:macro use-fixtures [_ _ type & fns]
  (condp = type
    :once
    `(def ~'cljs-test-once-fixtures
       [~@fns])
    :each
    `(def ~'cljs-test-each-fixtures
       [~@fns])
    :else
    (throw
     (js/Error. "First argument to cljs.test/use-fixtures must be :once or :each"))))

(defn successful?
  "Returns true if the given test summary indicates all tests
  were successful, false otherwise."
  {:added "1.1"}
  [summary]
  (and (zero? (:fail summary 0))
       (zero? (:error summary 0))))
