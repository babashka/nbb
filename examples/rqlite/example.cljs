(ns example
  (:require ["rqlite-js$default" :as rq]
            [promesa.core :as p]))

(def client (rq/DataApiClient. "http://localhost:4001"))

(defn query-by-name [name]
  (str "SELECT id FROM foo WHERE name = \"" name "\""))

(p/let [results (.query client (query-by-name "fiona"))]
  (when-not (.hasError results)
    (-> results
        (.get 0)
        (.get "id")
        (prn))))

;; should result in printing number 1 to the console followed by #<Promise[~]>
