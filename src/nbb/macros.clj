(ns nbb.macros)

(defmacro with-async-bindings [m & body]
  `(do (sci.impl.vars/push-thread-bindings ~m)
       (.finally (do ~@body)
                 (fn []
                   (sci.impl.vars/pop-thread-bindings)))))
