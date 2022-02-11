(ns example
  (:require [promesa.core :as p]))

(p/let [response (js/fetch "https://clojure.org")
        status (.-status response)
        body (.text response)]
  (prn status) ;; => 200
  (prn (subs body 0 10))) ;; => "<!DOCTYPE "
