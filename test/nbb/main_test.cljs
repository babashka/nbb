(ns nbb.main-test
  (:require
   ["node:module" :refer [createRequire]]
   ["node:path" :as path]
   ["node:process" :as process]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [nbb.classpath :as cp]
   [nbb.core :as nbb]
   [nbb.impl.main :as main]
   [nbb.test-test]
   [sci.core :as sci])
  (:require-macros [nbb.test-macros :refer [deftest-async]]))

(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

(def old-fail (get-method t/report [:cljs.test/default :fail]))

(defmethod t/report [:cljs.test/default :fail] [m]
  (set! process/exitCode 1)
  (old-fail m))

(def old-error (get-method t/report [:cljs.test/default :fail]))

(defmethod t/report [:cljs.test/default :error] [m]
  (set! process/exitCode 1)
  (old-error m))

(cp/add-classpath "test-scripts")
(reset! nbb/ctx {:require (createRequire (path/resolve "script.cljs"))})

;; NOTE: CLJS only accepts one async + done per deftest
;; See https://clojurescript.org/tools/testing#async-testing.

(defn with-args [args f]
  (let [old-args (or js/globalThis.nbb_args process/argv)
        args (into-array (list* nil nil args))]
    (set! js/globalThis.nbb_args args)
    (-> (f)
        (js/Promise.resolve)
        (.finally (fn []
                    (set! js/globalThis.nbb_args old-args))))))

(defn main-with-args [args]
  (with-args args main/main))

(deftest parse-args-test
  (is (= {:expr "(+ 1 2 3)"} (main/parse-args ["-e" "(+ 1 2 3)"])))
  (is (= {:script "foo.cljs", :args nil} (main/parse-args ["foo.cljs"])))
  (is (= {:script "foo.cljs", :args '("1" "2" "3")} (main/parse-args ["foo.cljs" "1" "2" "3"])))
  (is (= {:classpath "src", :script "foo.cljs", :args nil} (main/parse-args ["--classpath" "src" "foo.cljs"])))
  (is (= {:expr "(require 'foo) (apply foo/-main *command-line-args*)", :args '("1" "2" "3")}
         (main/parse-args ["-m" "foo" "1" "2" "3"])))
  (is (= {:nrepl-server true, :port "0.0.0.0"}
      (main/parse-args ["nrepl-server" "--port" "0.0.0.0" ])))
  (is (= {:config "../foo/nbb.edn"}
         (main/parse-args ["--config" "../foo/nbb.edn"]))))

(deftest-async simple-require-test
  (-> (nbb/load-string "(ns foo (:require cljs.core clojure.set))
                        (and (some? cljs.core/inc) (some? clojure.set/union))")
      (.then (fn [v]
               (is (true? v))))))

(deftest-async dynamic-require-test
  (-> (nbb/load-string "(when (odd? 3)
                          (require '[promesa.core :as p]))
                        (p/do (p/delay 20) :hello)")
      (.then (fn [v]
               (is (= :hello v))))))

(deftest-async as-alias
  (-> (nbb/load-string "(require '[rando.ns :as-alias dude]) ::dude/foo")
      (.then (fn [v]
               (is (= :rando.ns/foo v))))))

(deftest-async load-string-file-test
  (-> (nbb/load-string "(ns foo) (defn foo [] (+ 1 2 3)) (ns-name *ns*)")
      (.then (fn [ns-name]
               (is (= 'foo ns-name))))
      (.then (fn [_] (nbb/load-string
                      "(nbb.core/load-string \"(ns bar) (defn foo [] (+ 1 2 3)) (ns-name *ns*)\")")))
      (.then (fn [ns-name]
               (testing "internal load-string"
                 (is (= 'bar ns-name)))))
      (.then (fn [_]
               (sci/binding [sci/ns (sci/create-ns 'user)]
                 (nbb/load-string "(ns-name *ns*)"))))
      (.then (fn [ns-name]
               (prn :nsss ns-name)
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

(deftest-async main-test
  {:before (set! *print-fn* (constantly nil))
   :after (set! *print-fn* pf)}
  (-> (main-with-args ["-m" "clojure.core/str" "1" "2" "3"])
      (.then (fn [res]
               (is (= "123" res))))))

(defn normalize-filename [s]
  (str/replace s "\\" "/"))

(deftest-async load-file-test
  (-> (main-with-args ["test-scripts/load_file_test.cljs"])
      (.then (fn [res]
               (let [f (normalize-filename (:file res))]
                 (is (path/isAbsolute f))
                 (is (str/ends-with? f "test-scripts/loaded_by_load_file_test.cljs")))
               (is (:loaded-by-load-file-test/loaded res))
               (is (= (:file res) (:file-via-dyn-var res)))
               (let [f (normalize-filename (:load-file-test-file-dyn-var res))]
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

(deftest-async gobject-get-all-property-names
  (-> (nbb/load-string "(ns foo (:require [goog.object :as gobj]))
                        (gobj/getAllPropertyNames #js{:x 0 :y 1 :z 2})")
      (.then (fn [val]
               (is (= ["x" "y" "z"] (js->clj val)))))))

(deftest-async gobject-remove
  (-> (nbb/load-string "(ns foo (:require [goog.object :as gobj]))
                        (def obj #js {:x 0 :y 1})
                        (gobj/remove obj \"x\")
                        obj")
      (.then (fn [val]
               (is (= {"y" 1} (js->clj val)))))))

(deftest goog-object-ns
  (testing "every value in goog-object-ns is a function"
    (is (every? ifn? (vals @#'nbb/goog-object-ns))))

  (testing "the expected number of functions are included"
    (is (= 35 (count @#'nbb/goog-object-ns)))))

(deftest-async with-out-str-test
  (-> (nbb/load-string "(require '[cljs.pprint :as pp])
[(with-out-str (println :hello))
 (with-out-str (prn :hello))
 (with-out-str (print :hello))
 (with-out-str (pp/print-table [{:a 1} {:a 2}]))]")
      (.then (fn [val]
               (is (= [":hello\n" ":hello\n" ":hello" "\n| :a |\n|----|\n|  1 |\n|  2 |\n"]
                      val))))))

(deftest-async no-op-vars-test
  (-> (nbb/load-string "[*warn-on-infer* (set! *warn-on-infer* true)]")
      (.then (fn [val]
               (is (vector? val))))))

(deftest-async reader-conditional-test
  (-> (nbb/load-string "#?(:org.babashka/nbb 1 :cljs 2)")
      (.then (fn [val]
               (is (= 1 val))))))

(deftest-async def-await-test
  (-> (nbb/load-string
       (pr-str '(do (require '[nbb.core :refer [await]])
                    (require '[promesa.core :as p])
                    (def x (await (p/do (p/delay 100) :hello)))
                    (= x :hello))))
      (.then (fn [val]
               (is (true? val))))))

(deftest-async await-test
  (-> (nbb/load-string
       (pr-str '(do (require '[nbb.core :refer [await]])
                    (require '[promesa.core :as p])
                    (def x (atom nil))
                    (await (p/do (p/delay 100) (reset! x :foo)))
                    (= :foo @x))))
      (.then (fn [val]
               (is (true? val))))))

(deftest-async require-macros-test
  (-> (main-with-args ["test-scripts/require_macros.cljc"])
      (.then (fn [res]
               (is (= {:add 5 :subtract 1} res))))))

(deftest-async stack-consumption-test
  (-> (nbb/load-string (str (str/join "\n" (map str (repeat 10000 nil))) 100))
      (.then (fn [res]
               (is (= 100 res))))
      (.then (fn [_]
               (nbb/load-string (str/join "\n" (map str (repeat 10000 '(+ 1 2 3)))))))
      (.then (fn [res]
               (is (= 6 res))))))

(deftest-async queue-test
  (-> (.then (nbb/load-string "#queue [1 2 3]")
             (fn [val]
               (is (= #queue [1 2 3] val))))
      (.then (nbb/load-string "(into cljs.core/PersistentQueue.EMPTY [1 2 3])")
             (fn [val]
               (is (= #queue [1 2 3] val))))))

(deftest-async print-error-report-test
  (-> (nbb/load-string "
(require '[nbb.error :refer [print-error-report]]
         '[clojure.string :as str])

(def result
  (try
    (assoc :foo :bar)
    (catch ^:sci/error js/Error e
      (let [lines (atom [])]
        (binding [*print-err-fn* #(swap! lines conj %)]
          (print-error-report e))
        (str/join \"\n\" @lines)))))

result")
      (.then (fn [val]
               (is (str/includes? val "Message:  No protocol method IAssociative"))
               (is (str/includes? val "----- Error -----"))))))

(deftest-async do-simple-test
  (is (.then (nbb/load-string "(do (def x (nbb.core/await (js/Promise.resolve 1))) x)")
             (fn [val]
               (is (= 1 val))))))

(deftest-async atom-instance-test
  (is (.then (nbb/load-string "(instance? cljs.core/Atom (atom {}))")
             (fn [val]
               (is (true? val))))))

(deftest-async macro-with-referred-js-function-test
  (is (.then (nbb/load-string "(ns foo (:require [\"fs\" :refer [readFileSync]]))
(defmacro read [f] `(readFileSync ~f \"UTF-8\"))
(read \"README.md\")")
             (fn [val]
               (is (string? val))))))

(deftest-async macro-with-aliased-lib-test
  (is (.then (nbb/load-string "(ns foo (:require [\"fs\" :as fs]))
(defmacro read [f] `(fs/readFileSync ~f \"UTF-8\"))
(ns bar (:require [foo]))
(foo/read \"README.md\")")
             (fn [val]
               (is (string? val))))))

(deftest-async macro-with-aliased-lib-and-default-test
  (is (.then (nbb/load-string "(ns scratch
  (:require [\"moment$default\" :as moment]))

(defmacro now []
  `(.now moment))

(ns yolo
  (:require [scratch]))

(scratch/now)")
             (fn [val]
               (is (number? val))))))

(deftest-async reload-test
  (is (.then (nbb/load-string "(require 'reload)
(require 'reload :reload)

[@reload/my-atom reload/x]")
             (fn [val]
               (is (= [2 2] val))))))

(defn init []
  (t/run-tests 'nbb.main-test 'nbb.test-test))

