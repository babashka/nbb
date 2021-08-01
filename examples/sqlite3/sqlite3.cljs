(ns script.sqlite
  (:require ["sqlite3" :as sqlite]))

(def db (sqlite/Database. ":memory:"))

(.serialize
 db
 (fn []
   (.run db "CREATE TABLE lorem (info TEXT)")
   (let [stmt (.prepare db "INSERT INTO lorem VALUES (?)")]
     (dotimes [i 10]
       (.run stmt (str i)))
     (.finalize stmt))
   (.each db "SELECT rowid AS id, info FROM lorem"
          (fn [_err row]
            (println (str (.-id row) ": " (.-info row)))))))

(.close db)
