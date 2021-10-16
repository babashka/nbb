(ns test-utils
  (:require [clojure.string :as str]
            [clojure.test :refer [*report-counters*]]))

(def windows? (-> (System/getProperty "os.name")
                  str/lower-case
                  (str/starts-with? "win")))


(defmethod clojure.test/report :end-test-var [_m]
  (when-let [rc *report-counters*]
    (let [{:keys [:fail :error]} @rc]
      (when (and (= "true" (System/getenv "FAIL_FAST"))
                 (or (pos? fail) (pos? error)))
        (println "=== Failing fast")
        (System/exit 1)))))
