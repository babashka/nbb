(ns plet
  {:clj-kondo/config '{:lint-as {plet/plet clojure.core/let}}})

(defmacro plet
  "Inspired by https://github.com/mauricioszabo/repl-tooling/blob/b4962dd39b84d60cbd087a96ba6fccb1bffd0bd6/src/repl_tooling/editor_integration/interpreter.cljs#L26"
  [bindings & body]
  (let [binding-pairs (reverse (partition 2 bindings))
        body (list* 'do body)]
    ;; (prn binding-pairs)
    (reduce (fn [body [sym expr]]
              (let [expr (list '.resolve 'js/Promise expr)]
                (list '.then expr (list 'clojure.core/fn (vector sym)
                                        body))))
            body
            binding-pairs)))


(def without-destructuring
  (plet [html (.resolve js/Promise "<!DOCTYPE html><html>hello</html>")
         substring (subs html 0 20)
         x 1
         y 2]
        [x y substring]))

(plet [x without-destructuring
       {:keys [:a]} {:a 1}]
      (conj x a))
