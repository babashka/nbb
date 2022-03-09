(ns example
  (:require ["gulp$default" :refer [src dest]]
            ["gulp-babel$default" :as babel]))

(defn default []
  (-> (src "gulpfile.mjs")
      (.pipe (babel))
      (.pipe (dest "output/"))))

default
