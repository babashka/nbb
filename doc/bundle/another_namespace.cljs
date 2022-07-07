(ns another-namespace
  (:require
   ["term-size$default" :as term-size]
   [cljs-bean.core :as bean]
   [utils :as u]))

(defn cool-fn []
  [(u/util-fn) (bean/bean (term-size))])
