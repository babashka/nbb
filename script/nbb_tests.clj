(ns nbb-tests
  (:require
   [babashka.classpath :as cp]
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.process :refer [check process]]
   [babashka.tasks :as tasks]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [nbb-nrepl-tests]
   [nbb-repl-tests]
   [test-utils :as tu :refer [windows?]]))

(defmethod clojure.test/report :begin-test-var [m]
  (println "===" (-> m :var meta :name))
  (println))

(def normalize
  (if windows?
    (fn [s] (if (string? s)
              (str/replace s "\r\n" "\n")
              s))
    identity))

(defn nbb** [x & xs]
  (let [[opts args] (if (map? x)
                      [x xs]
                      [nil (cons x xs)])
        dir (:dir opts)
        rel (when dir (fs/relativize dir "."))]
    (process (into ["node" (fs/path rel "lib/nbb_main.js")] args)
             (merge {:out :string
                     :err :inherit}
                    opts))))

(defn nbb* [& xs]
  (-> (apply nbb** xs)
      check
      :out
      normalize))

(defn nbb [& args]
  (let [res (apply nbb* args)]
    (when (string? res)
      (edn/read-string res))))

(deftest expression-test
  (is (= 6 (nbb "-e" "(+ 1 2 3)")))
  (testing "nil doesn't print return value"
    (is (= (str "6\n") (nbb* "-e" "(prn (+ 1 2 3))")))))

(defn npm [cmd]
  (str (if windows?
         "npm.cmd" "npm")
       " " cmd))

(deftest react-and-ink-test
  (tasks/shell {:dir "test-scripts/react-test"} (npm "install"))
  (is (= "<div><a>1</a></div>" (nbb "test-scripts/react-test/dom-server-test.cljs")))
  (testing "react is loaded first, then reagent"
    (nbb {:out :inherit} "test-scripts/react-test/ink-test.cljs"))
  (testing "reagent is loaded first, then react"
    (nbb {:out :inherit} "test-scripts/react-test/ink-test2.cljs"))
  (is (= "<div data-reactroot=\"\"><p>Hi</p></div>"
         (nbb "test-scripts/react-test/create-class-test.cljs"))))

(deftest esm-libs-test
  (tasks/shell {:dir "test-scripts/esm-test"} (npm "install"))
  (nbb {:out :inherit} "test-scripts/esm-test/script.cljs"))

(deftest issue-80-test
  (testing "ns form can be evaluated multiple times"
    (tasks/shell {:dir "test-scripts/issue-80"} (npm "install"))
    (nbb {:out :inherit} "test-scripts/issue-80/script.cljs")))

(deftest chalk-test
  (tasks/shell {:dir "examples/chalk"} (npm "install"))
  (nbb {:out :inherit} "examples/chalk/example.cljs"))

(defn make-prog [exprs]
  (str/join "\n" (map pr-str exprs)))

(deftest promesa-test
  (is (= 2 (nbb "-e" "(require '[promesa.core :as p])
                      (p/let [x (js/Promise.resolve 1)] (+ x 1))")))
  (is (= [:bar :foo]
         (nbb "-e"
              (make-prog
               '[(require '[promesa.core :as p])
                 (def result (atom []))
                 (defn foo [] :foo)
                 (->
                  (p/do!
                   (p/with-redefs [foo (fn [] :bar)]
                     (swap! result conj (foo)))
                   (swap! result conj (foo))
                   @result))])))))

(deftest classpath-test
  (testing "passing classpath cli arg"
    (let [deps '{com.github.seancorfield/honeysql {:git/sha "23be700"
                                                   :git/tag "v2.3.928"}}
          _ (deps/add-deps {:deps deps})
          cp (cp/get-classpath)]
      (is (= ["SELECT foo FROM bar WHERE baz = ?" 2]
             (nbb "--classpath" cp
                  "-e"
                  "(require '[honey.sql :as sql]) (sql/format {:select :foo :from :bar :where [:= :baz 2]})")))))
  (testing "add `:paths` from nbb.edn to classpath"
    (is (= "success"
         (nbb {:dir "test-scripts/paths-test"} "runner.cljs"))))
  (testing "project dir is removed from classpath when `:paths` present in `nbb.edn`"
    (is (thrown? Exception
                 (nbb {:dir "test-scripts/paths-test"} "src/project_dir_not_on_classpath.cljs")))))

(deftest invoked-file-test
  (testing "calling as a script"
    (is (= :invoked
           (nbb
            {:dir "test-scripts/invoked-file-test"}
            "src/script.cljs"))))
  (testing "calling with -m"
    (is (= :not-invoked
           (nbb {:dir "test-scripts/invoked-file-test"}
                "-m" "core"))))
  (testing "calling with -e"
    (is (= :not-invoked
           (nbb "-e" "(require '[nbb.core :refer [*file* invoked-file]])
(if (and (some? *file*) (= *file* (invoked-file)))
  :invoked
  :not-invoked)")))))

(deftest medley-test
  (let [deps '{medley/medley {:git/url "https://github.com/weavejester/medley"
                              :git/tag "1.4.0"
                              :git/sha "0044c6a"}}
        _ (deps/add-deps {:deps deps})
        cp (cp/get-classpath)]
    (is (= {:a {:id :a}, :b {:id :b}}
           (nbb "--classpath" cp
                "-e"
                "(require '[medley.core :as m]) (m/index-by :id [{:id :a} {:id :b}])")))))

(deftest pprint-test
  (testing "pprint"
    (is (= (str "(0 1 2 3 4 5 6 7 8 9)\n")
          (nbb "-e" "(require '[cljs.pprint :as pp]) (with-out-str (pp/pprint (range 10)))"))))
  (testing "cljs.pprint = clojure.pprint"
    (is (= (str "(0 1 2 3 4 5 6 7 8 9)\n")
           (nbb "-e" "(require '[clojure.pprint :as pp]) (with-out-str (pp/pprint (range 10)))"))))
  (testing "print-table"
    (is (= "\n| :a |\n|----|\n|  1 |\n|  2 |\n"
           (nbb* "-e" "(require '[clojure.pprint :as pp]) (do (pp/print-table [{:a 1} {:a 2}]))")))))

(deftest data-test
  (is (= '({:a 1} {:c 3} {:b 2})
         (nbb "-e" "(require '[clojure.data :as data]) (data/diff {:a 1 :b 2} {:b 2 :c 3})"))))

(deftest api-test
  (tasks/shell {:dir "test-scripts/api-test"} (npm "install"))
  (tasks/shell {:dir "test-scripts/api-test"} "node test.mjs"))

(defn normalize-interop-output
  "Functions print differently in compile vs release."
  [s]
  (str/replace s #"object\[.*\]" "object[-]"))

(def expected-js-interop-output (normalize-interop-output "#js {:y 1, :someFn #object[Function]}
1
#js {:a 1, :b 2, :c 3}
#js {:y 1, :someFn #object[Function]}
[:a 1 :b 2 :c 3]
[1 2]
[1 2 3]
[1 2 3]
#js {:x #js {:y 1, :someFn #object[Function]}, :a 2, :b 2, :c 3, :someFn #object[Function]}
#js {:x #js {:y 100, :someFn #object[Function]}, :a 2, :b 2, :c 3, :someFn #object[Function]}
#js {:x #js {:y 100, :someFn #object[Function]}, :a 3, :b 2, :c 3, :someFn #object[Function]}
#js {:x #js {:y 110, :someFn #object[Function]}, :a 3, :b 2, :c 3, :someFn #object[Function]}
42
42
42
42
#js {:a 1, :b 2}
#js {:a 1, :b #js [2 3 4]}
"))

(deftest js-interop-test
  (is (= expected-js-interop-output
         (normalize-interop-output (nbb* "examples/js-interop/example.cljs")))))

(def expected-cljs-bean-output (normalize-interop-output "{:y 1, :someFn #object[-]}
1
{:a 1, :b 2, :c 3}
{:y 1, :someFn #object[-]}
[:a 1 :b 2 :c 3]
{:x {:y 1, :someFn #object[-]}
{:x {:y 100, :someFn #object[-]}
{:x {:y 1, :someFn #object[-]}
{:x {:y 11, :someFn #object[-]}
42
42
42
42
"))

(deftest cljs-bean-test
  (is (= expected-cljs-bean-output
         (normalize-interop-output (nbb* "examples/cljs-bean/example.cljs")))))

(deftest transit-test
  (is (= {:fruits [:apple :banana :pear]}
         (nbb "test-scripts/transit.cljs" (pr-str {:fruits [:apple :banana :pear]})))))

(deftest error-test
  (let [err (-> (process ["node" "lib/nbb_main.js" "test-scripts/error.cljs"]
                         {:out :string
                          :err :string})
                deref
                :err
                normalize)]
    (is (str/includes? err "5: (assoc :x :y 1)"))
    (is (str/includes?
         err
         "   ^--- No protocol method IAssociative.-assoc defined"))
    (is (str/includes?
         err
         "   ^--- No protocol method IAssociative.-assoc defined"))
    (is (str/includes? err "clojure.core/assoc - <built-in>"))
    (is (str/includes? err "error.cljs:5:1"))))

(deftest exit-code-test
  (is (not
       (zero? (-> (nbb** "-e" "printt")
                  deref
                  :exit)))))

(deftest failing-test-results-in-failing-exit-code
  (is (not
       (zero?
        (-> (nbb** "-e" "
    (ns foo0 (:require [clojure.test :as t :refer [deftest is testing]]))
    (t/deftest foo (t/is (= 1 2))) (t/run-tests 'foo0)")
            deref
            :exit)))))

(deftest local-js-require-test
  (is (= {:a 1, :other.script/foo 1
          :nss ["foo" "bar" "baz" "user"]}
         (nbb {:dir "test-scripts/local-js"} "-cp" "src" "-m" "script"))))

(deftest malli-test
  (is (= [{:prop "-server", :val [:s "foo"]} {:prop "-verbose", :val [:b true]} {:prop "-user", :val [:s "joe"]}]
         (nbb {:dir "examples/metosin-malli"} "example.cljs"))))

(deftest default-classpath-test
  (let [o (nbb* {:dir "test-scripts/default_classpath_test"} "test.cljs")]
    (is (str/includes? o "TEST1"))
    (is (str/includes? o "BAR"))))

(deftest local-root-test
  (let [o (nbb* "test-scripts/local-root-test/run.cljs")]
    (is (str/includes? o "{:a 1}"))))

(defn parse-opts [opts]
  (let [[cmds opts] (split-with #(not (str/starts-with? % ":")) opts)]
    (into {:cmds cmds}
          (for [[arg-name arg-val] (partition 2 opts)]
            [(keyword (subs arg-name 1)) arg-val]))))

(defn main [& args]
  (let [opts (parse-opts args)
        {:keys [error fail]}
        (if (empty? (dissoc opts :cmds))
          (t/run-tests 'nbb-tests 'nbb-nrepl-tests 'nbb-repl-tests)
          (when-let [o (:only opts)]
            (let [o (symbol o)]
              (if (qualified-symbol? o)
                (do
                  (println "Testing" o)
                  (binding [t/*report-counters* (ref t/*initial-report-counters*)]
                    (t/test-var (resolve o))
                    @t/*report-counters*))
                (t/run-tests o)))))]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
