(ns test-utils
  (:require [clojure.string :as str]))

(def windows? (-> (System/getProperty "os.name")
                  str/lower-case
                  (str/starts-with? "win")))

