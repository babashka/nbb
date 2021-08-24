(ns nbb.main-test
  (:require ["path" :as path]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest is testing]]
            [nbb.core :as nbb]
            [nbb.main :as main])
  (:require-macros [nbb.test-macros :refer [deftest-async]]))

(reset! nbb/ctx {:require js/require
                 :classpath {:dirs ["test-scripts"]}})

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

(deftest-async load-string-file-test
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
               (nbb/load-file "test-scripts/script.cljs")))
      (.catch (fn [err]
                (println err (.-stack err))))
      (.then (fn [val]
               (is (= 6 val))))
      (.then (fn [_]
               (nbb/load-string "(nbb.core/load-file \"test-scripts/script.cljs\")")))
      (.then (fn [val]
               (is (= 6 val))))))

(def pf *print-fn*)

(deftest-async args-test
  {:before (set! *print-fn* (constantly nil))
   :after (set! *print-fn* pf)}
  (-> (main-with-args ["test-scripts/script.cljs"])
      (.then (fn [res]
               (is (= 6 res))))
      (.then (fn [_]
               (main-with-args ["-e" "(+ 1 2 3 4)"])))
      (.then (fn [res]
               (is (= 10 res))))
      (.then (fn [_]
               (main-with-args["-e" "(nbb.core/load-file \"test-scripts/script.cljs\")"])))
      (.then (fn [res]
               (is (= 6 res))))))

(deftest-async load-file-test
  (-> (main-with-args ["test-scripts/load_file_test.cljs"])
      (.then (fn [res]
               (let [f (:file res)]
                 (is (path/isAbsolute f))
                 (is (str/ends-with? f "test-scripts/loaded_by_load_file_test.cljs")))
               (is (:loaded-by-load-file-test/loaded res))
               (is (= (:file res) (:file-via-dyn-var res)))
               (let [f (:load-file-test-file-dyn-var res)]
                 (is (path/isAbsolute f))
                 (is (str/ends-with? f "test-scripts/load_file_test.cljs" )))))))

(deftest-async eval-string-test
  (-> (nbb/load-string "(+ 1 2 3)")
      (.then (fn [res]
               (is (= 6 res))))
      (.then (fn [_]
               (main-with-args ["test-scripts/plet.cljs"])))
      (.then (fn [res]
               (is (= [1 2 "<!DOCTYPE html><html" 1] res))))))

(deftest-async require-built-in-namespace-test
  {:before (set! *print-fn* (constantly nil))
   :after (set! *print-fn* pf)}
  (-> (main-with-args ["-e"
                       "(require '[clojure.string :as s :refer [includes?] :rename {includes? inc?}])
                               [(some? s/replace) (some? inc?) (= inc? s/includes?)]"])
      (.then (fn [res]
               (is (= [true true true] res))))))

(deftest-async require-node-module-test
  {:before (set! *print-fn* (constantly nil))
   :after (set! *print-fn* pf)}
  (-> (main-with-args ["-e"
                       "(require '[\"fs\" :as fs :refer [existsSync] :rename {existsSync exists?}])
                               [(some? fs/existsSync) (some? exists?) (= exists? fs/existsSync)]"])
      (.then (fn [res]
               (is (= [true true true] res))))))

(deftest-async require-namespace-from-file-test
  {:before (set! *print-fn* (constantly nil))
   :after (set! *print-fn* pf)}
  (-> (main-with-args ["--classpath" "test-scripts" "-e"
                       "(require '[script :as s :refer [script-fn] :rename {script-fn f}])
                               [(s/script-fn) (f)]"])
      (.then (fn [res]
               (is (= [:hello :hello] res))))))

(deftest-async error-test
  (-> (nbb/load-string "(+ 1 2 3) (assoc 1 2)")
      (.catch (fn [err]
                (let [d (ex-data err)]
                  (is (= 1 (:line d)))
                  (is (= 11 (:column d))))))))

(deftest-async gobject-test
  (-> (nbb/load-string "(require '[goog.object :as gobj :refer [get] :rename {get jget}])
                        (def x #js {}) (gobj/set x \"x\" 1)
                        (gobj/set x \"y\" 2) (jget x \"y\")")
      (.then (fn [val]
               (is (= 2 val))))))

(deftest-async gobject-get-exclude-test
  (-> (nbb/load-string "(ns foo (:refer-clojure :exclude [get])
                                (:require [goog.object :as gobj :refer [get]]))
                        (get #js{:y 2} \"y\") ")
      (.then (fn [val]
               (is (= 2 val))))))

(deftest-async with-out-str-test
  (-> (nbb/load-string "[(with-out-str (println :hello))
                         (with-out-str (prn :hello))
                         (with-out-str (print :hello))]")
      (.then (fn [val]
               (is (= [":hello\n" ":hello\n" ":hello"]
                      val))))))

(defn init []
  (t/run-tests *ns*))
