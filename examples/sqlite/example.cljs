(ns example
  (:require
   ["sqlite" :refer [open]]
   ["sqlite3$default" :as sqlite]
   [promesa.core :as p]))

(p/let [db (open #js {:filename ":memory:"
                      :driver sqlite/Database})
        _ (.run db "CREATE TABLE lorem (info TEXT)")
        stmt (.prepare db "INSERT INTO lorem VALUES (?)")]
  (p/all (map #(.run stmt (str %)) (range 10)))
  (.finalize stmt)
  (.each db "SELECT rowid AS id, info FROM lorem"
           (fn [_err row]
             (println (str (.-id row) ": " (.-info row)))))
  (.close db))
