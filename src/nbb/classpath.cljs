(ns nbb.classpath
  (:require #_["path" :as path]
            [clojure.string :as str]))

(def classpath-entries (atom []))

(defn get-classpath []
  #_(str/join path/delimiter @classpath-entries))

(defn split-classpath [cp]
  #_(str/split cp (re-pattern path/delimiter)))

(defn add-classpath [cp]
  (swap! classpath-entries into (split-classpath cp))
  nil)
