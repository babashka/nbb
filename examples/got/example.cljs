(ns example
  (:require ["got$default" :as got]
            [promesa.core :as p]
            [applied-science.js-interop :as j]))

(p/let [result (-> got
                   (.post "https://httpbin.org/anything" (clj->js {:json {:hello "world"}}))
                   .json)]
  (js/console.log (j/get result :data)))
