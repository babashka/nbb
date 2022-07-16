(ns from-classpath
  #?(:cljs (:require [cljs-on-classpath :as cp])))

#?(:cljs
   (defn from-classpath []
     #?(:cljs (cp/hello))))
