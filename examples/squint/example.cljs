(ns example
  (:require ["squint-cljs/index.js" :as squint]))

;; we're compiling a JS function using squint for performance
(def js-code (squint/compileString
              (pr-str
               '(let [f (fn []
                          (loop [val 0 cnt 10000000]
                            (if (< 0 cnt)
                              (recur
                               (inc val)
                               (dec cnt))
                              val)))]
                  f))))

(def js-fn (js/eval js-code))

;; running the compiled function with 10M iterations takes about 13ms
(prn (time (js-fn)))

;; running the same example within the SCI interpreter takes more than 0.5s
(prn (time (loop [val 0 cnt 10000000]
             (if (< 0 cnt)
               (recur
                (inc val)
                (dec cnt))
               val))))
