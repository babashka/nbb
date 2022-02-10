(ns example
  (:require [promesa.core :as p]
            [applied-science.js-interop :as j]))

(p/let [response (js/fetch "https://clojure.org")
        status (j/get response :status)
        body (.text response)]
  (prn status) ;; => 200
  (prn (subs body 0 10))) ;; => "<!DOCTYPE "
