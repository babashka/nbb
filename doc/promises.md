# Promises

Working with callbacks and promises can become tedious. A few helper macros can make life easier.

Consider `plet` and `pdo` below:

``` clojure
(ns promises
  {:clj-kondo/config '{:lint-as {promises/plet clojure.core/let}}})

(defmacro plet
  [bindings & body]
  (let [binding-pairs (reverse (partition 2 bindings))
        body (cons 'do body)]
    ;; (prn binding-pairs)
    (reduce (fn [body [sym expr]]
              (let [expr (list '.resolve 'js/Promise expr)]
                (list '.then expr (list 'clojure.core/fn (vector sym)
                                        body))))
            body
            binding-pairs)))

(defmacro pdo [& body]
  (let [wrap-resolve (fn [v] (list '.resolve 'js/Promise v))
        exprs (map (fn [expr]
                     (list '.then
                           (list 'clojure.core/fn '[_]
                                 (wrap-resolve expr))))
                   (rest body))]
    `(-> ~(wrap-resolve (first body)) ~@exprs)))

(defn sleep [ms]
  (js/Promise.
   (fn [resolve]
     (js/setTimeout resolve ms))))


(defn do-stuff
  "Returns map in promise"
  []
  (pdo
   (prn :start)
   (sleep 1000)
   (prn :awake)
   (sleep 1000)
   (prn :awake-again)
   (assoc {} :a 1)))

(plet [{:keys [a]} (do-stuff)
       b (+ a 42)]
      (prn :b b))
```

This will print:

```
:start
:awake
:awake-again
:b 43
```
