(ns sqlite
  (:require ["bun:sqlite$default" :as sqlite]))

(defn select []
  (let [db (new sqlite ":memory:")]
    (prn (.get (.query db "select 'Bun' as runtime")))))

(select)
