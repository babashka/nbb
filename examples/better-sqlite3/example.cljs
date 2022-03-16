(ns example
  (:require
    ["better-sqlite3$default" :as sqlite]))

(let [db (new sqlite ":memory:")
      _  (-> db (.prepare "CREATE TABLE lorem (info TEXT)") .run)
      stmt (.prepare db "INSERT INTO lorem VALUES (?)")]
  (doall (map #(.run stmt (str %)) (range 10)))
  (doall (map #(println (str (.-id %) ": " (.-info %)))
              (-> db (.prepare "SELECT rowid AS id, info FROM lorem") .all)))
  (.close db))

