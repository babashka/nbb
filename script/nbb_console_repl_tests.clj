(ns nbb-console-repl-tests
  (:require
   [babashka.process :as p :refer [process]]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is]]))

(defn repl [input]
  (-> (process ["node" "out/nbb_main.js"]
               {:out :string
                :in input
                :err :inherit})
      p/check))

(deftest repl-test
  (is (str/includes? (:out (repl "(+ 1 2 3)")) "6\n"))
  (is (str/includes? (:out (repl "(ns foo) (str *ns*)")) "foo> "))
  (is (str/includes?
       (:out (repl "(ns foo (:require [\"fs\" :as fs])) (fs/existsSync \"README.md\")"))
       "true\n")))

(defn -main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-console-repl-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
