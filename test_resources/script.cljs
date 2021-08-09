(ns test-resources.script
  (:require ["fs" :as fs] ;; verify that required namespaces can in turn also load node modules
            [nbb.core] ;; built in namespace, continue processing libspecs, GH-36
            [test-resources.other-script :as o :refer [another-fn] :rename {another-fn foo}]))

(defn script-fn []
  (fs/existsSync ".")
  (when (and (= :yolo (o/script-fn))
             (= :another-fn (foo)))
    :hello))

(+ 1 2 3)
