(ns nbb.impl.clojure.test)

(defmacro with-test-out-internal
  "Runs body with *out* bound to the value of *test-out*."
  {:added "1.1"}
  [& body]
  `(do ~@body))
