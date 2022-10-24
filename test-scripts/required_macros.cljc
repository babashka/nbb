(ns required-macros)

(defmacro do2 [& body]
  `(do ~@body ~@body))
