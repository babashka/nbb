(ns example
  (:require ["PouchDB$default" :as pouch]
            [promesa.core :as p]
            [cljs-bean.core :refer [->clj ->js]]))

;; PouchDB intro: https://pouchdb.com/learn.html

;; PouchDB helper functions
(defn pouch-put [db doc]
  (.put db (->js doc)))

(defn pouch-get [db id]
  (p/let [doc-js (.get db id)]
    (->clj doc-js)))

(defn pouch-rm [db doc]
  (.remove db (->js doc)))

;; Create a DB if not exist
(def db (pouch. "kittens"))

(def mittens
  {:_id "mittens"
   :name "Mittens"
   :occupation :kitten
   :age 4
   :hobbies ["playing with balls of yarn"
             "chasing laser pointers"
             "lookin' hella cute"]})


(comment
  ;; Store Mittens in our DB
  (pouch-put db mittens)

  ;; increase the age of Mittens
  (p/let [doc (pouch-get db "mittens")
          _ (pouch-put db (update-in doc [:age] inc))
          doc (pouch-get db "mittens")]
    (prn doc))

  ;; remove Mittens from DB
  (p/->> (pouch-get db "mittens")
         (pouch-rm db))
  )
