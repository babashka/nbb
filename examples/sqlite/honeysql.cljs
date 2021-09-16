(ns example
  (:require
   ["sqlite$default" :refer [open]]
   ["sqlite3$default" :as sqlite]
   [honey.sql :as sql]
   [promesa.core :as p]))

(p/let [db (open #js {:filename ":memory:"
                      :driver sqlite/Database})]
  (.run db (first (sql/format {:create-table :lorem
                                 :with-columns [[:info :text]]})))
  (p/let [[query & args] (sql/format {:insert-into :lorem
                                        :columns [:info]
                                        :values (map vector (range 1 11))})
            stmt (.prepare db query)]
      (.run stmt (into-array args))
      (.finalize stmt))
  (.each db (first (sql/format {:select [[:rowid :id] :info]
                                :from :lorem}))
         (fn [_err row]
           (println (str (.-id row) ": " (.-info row)))))
  (.close db))

