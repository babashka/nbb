(ns nbb.macros
  (:require [clojure.data.json :as json]))

(defmacro with-async-bindings [m & body]
  `(do (sci.impl.vars/push-thread-bindings ~m)
       (.finally (do ~@body)
                 (fn []
                   (sci.impl.vars/pop-thread-bindings)))))

(defmacro get-in-package-json [k]
  (get (json/read-str (slurp "package.json") :key-fn keyword) k))
