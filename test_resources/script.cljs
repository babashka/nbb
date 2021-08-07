(ns test-resources.script
  (:require ["fs" :as fs] ;; verify that required namespaces can in turn also load node modules
            [test-resources.other-script :as o]))

(defn script-fn []
  (fs/existsSync ".")
  (when (= :yolo (o/script-fn))
    :hello))

(+ 1 2 3)
