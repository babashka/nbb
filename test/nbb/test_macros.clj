(ns nbb.test-macros)

(defmacro deftest-async [name & body]
  `(cljs.test/deftest ~name
     (cljs.test/async
      ~'done
      (-> ~@body
          (.catch (fn [err#]
                    (cljs.test/is (= 1 0) (str err#))))
          (.finally ~'done)))))
