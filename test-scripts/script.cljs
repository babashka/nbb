(ns script
  (:require ["node:fs" :as fs] ;; verify that required namespaces can in turn also load node modules
            [nbb.core] ;; built in namespace, continue processing libspecs, GH-36
            [other-script :as o :refer [another-fn] :rename {another-fn foo}]))

(defn script-fn []
  (fs/existsSync ".")
  (when (and (= :yolo (o/script-fn))
             (= :another-fn (foo)))
    :hello))

(+ 1 2 3)
