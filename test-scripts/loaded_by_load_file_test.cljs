(ns loaded-by-load-file-test
  (:require [nbb.core :refer [*file*]]))

(def x)

(assoc (meta #'x) ::loaded true :file-via-dyn-var *file*)
