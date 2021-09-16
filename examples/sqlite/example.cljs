(ns example
  (:require
   ["sqlite$default" :refer [open]]
   ["sqlite3$default" :as sqlite]
   [promesa.core :as p]))

(p/let [db (open #js {:filename ":memory:"
                      :driver sqlite/Database})
        _ (.run db "CREATE TABLE lorem (info TEXT)")
        stmt (.prepare db "INSERT INTO lorem VALUES (?)")
        _ (p/all (map #(.run stmt (str %)) (range 10)))
        _ (.finalize stmt)
        _ (.each db "SELECT rowid AS id, info FROM lorem"
                 (fn [_err row]
                   (println (str (.-id row) ": " (.-info row)))))]
  (.close db))
