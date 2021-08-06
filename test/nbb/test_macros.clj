(ns nbb.test-macros)

(defmacro with-args [args done & body]
  `(let [old-args# js/process.argv
         args# (into-array (list* nil nil ~args))]
     (set! (.-argv js/process) args#)
     (-> (js/Promise.resolve (do ~@body))
         (.finally (fn []
                     (set! (.-argv js/process) old-args#)
                     (~done))))))

