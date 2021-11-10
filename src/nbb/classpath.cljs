(ns nbb.classpath
  (:require ["path" :as path]
            [clojure.string :as str]))

(def classpath-entries (atom []))

(defn get-classpath []
  (str/join path/delimiter @classpath-entries))

(defn split-classpath [cp]
  (str/split cp (re-pattern path/delimiter)))

(defn add-classpath [cp]
  (swap! classpath-entries into (split-classpath cp))
  nil)
