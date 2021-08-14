(ns axios.example
  (:require ["axios" :as axios]
            [promesa.core :as p]))

(p/let [resp (axios/get "https://clojure.org")
        resp (js->clj resp :keywordize-keys true)]
  (prn (:status resp)) ;; => 200
  (prn (subs (:data resp) 0 10))) ;; => "<!DOCTYPE "
