(ns nbb-tests
  (:require [babashka.classpath :as cp]
            [babashka.deps :as deps]
            [babashka.process :refer [process]]
            [clojure.edn :as edn]
            [clojure.test :as t :refer [deftest is testing]]))

(def nl (System/lineSeparator))

(defn nbb* [& args]
  (-> @(process (into ["node" "out/nbb_main.js" "--debug"] args) {:out :string
                                                                  :err :inherit})
      :out))

(defn nbb [& args]
  (edn/read-string (apply nbb* args)))

(deftest expression-test
  (is (= 6 (nbb "-e" "(+ 1 2 3)")))
  (testing "nil doesn't print return value"
    (is (= (str "6" nl) (nbb* "-e" "(prn (+ 1 2 3))")))))

(deftest reagent-test
  (is (= [true true]
         (testing "reagent works + refer and rename work on lazy loaded module"
           (nbb "-e" "(require '[reagent.core :as r :refer [atom] :rename {atom ratom}])
                    [(some? r/as-element) (some? ratom)]")))))

(deftest promesa-test
  (is (= 2 (nbb "-e" "(require '[promesa.core :as p])
                      (p/let [x (js/Promise.resolve 1)] (+ x 1))"))))

(deftest classpath-test
  (let [deps '{com.github.seancorfield/honeysql {:git/tag "v2.0.0-rc5" :git/sha "01c3a55"}}
        _ (deps/add-deps {:deps deps})
        cp (cp/get-classpath)]
    (is (= ["SELECT foo FROM bar WHERE baz = ?" 2]
           (nbb "--classpath" cp
                "-e"
                "(require '[honey.sql :as sql]) (sql/format {:select :foo :from :bar :where [:= :baz 2]})")))))

(deftest pprint-test
  (is (= (str "(0 1 2 3 4 5 6 7 8 9)" nl)
         (nbb "-e" "(require '[cljs.pprint :as pp]) (with-out-str (pp/pprint (range 10)))")))
  (testing "cljs.pprint = clojure.pprint"
    (is (= (str "(0 1 2 3 4 5 6 7 8 9)" nl)
           (nbb "-e" "(require '[clojure.pprint :as pp]) (with-out-str (pp/pprint (range 10)))")))))

(defn main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))

