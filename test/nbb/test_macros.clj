(ns nbb.test-macros)

(defmacro deftest-async [name & body]
  `(cljs.test/deftest ~name
     (cljs.test/async
      ~'done
      (-> ~@body
          (.catch (fn [err#]
                    (prn (str err#))))
          (.finally ~'done)))))
