(ns test-resources.script
  (:require ["fs" :as fs] ;; verify that required namespaces can in turn also load node modules
            [test-resources.other-script :as o :refer [another-fn]]))

(defn script-fn []
  (fs/existsSync ".")
  (when (and (= :yolo (o/script-fn))
             (= :another-fn (another-fn)))
    :hello))

(+ 1 2 3)
