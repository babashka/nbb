(ns example
  (:require ["sqlite3" :as sqlite]
            [honey.sql :as sql]))

(def db (sqlite/Database. ":memory:"))

(.serialize
 db
 (fn []
   (.run db (first (sql/format {:create-table :lorem
                                :with-columns [[:info :text]]})))
   (let [[query & args] (sql/format {:insert-into :lorem
                                     :columns [:info]
                                     :values (map vector (range 1 11))})
         stmt (.prepare db query)]
     (.run stmt (into-array args))
     (.finalize stmt))
   (.each db (first (sql/format {:select [[:rowid :id] :info]
                                 :from :lorem}))
          (fn [_err row]
            (println (str (.-id row) ": " (.-info row)))))))

(.close db)
