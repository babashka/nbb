(ns nbb-feature-tests
  "Test runner for feature tests"
  (:require [babashka.tasks :as tasks]
            [clojure.string :as str]
            [babashka.fs :as fs]))

(defn datascript-tests
  []
  (let [feature-dir "test-scripts/feature-tests/libraries/datascript"]
    (when-not (fs/exists? feature-dir)
      (tasks/shell "git clone -b 1.3.12 https://github.com/tonsky/datascript" feature-dir)
      (fs/copy "test-scripts/feature-tests/datascript_test_core.cljs"
               (str feature-dir "/test/datascript/test/core.cljc")
               {:replace-existing true}))
    (tasks/shell "node lib/nbb_main.js -cp"
                 (str feature-dir "/test")
                 "test-scripts/feature-tests/datascript_test_runner.cljs")))

(defn main []
  (let [features (some-> (System/getenv "NBB_FEATURES")
                         (str/split (re-pattern ","))
                         set)]
    (when (features "datascript")
      (datascript-tests))))
