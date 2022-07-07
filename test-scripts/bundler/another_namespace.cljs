(ns another-namespace
  (:require
   ["term-size$default" :as term-size]
   [cljs-bean.core :as bean]))

(defn cool-fn []
  (bean/bean (term-size)))
