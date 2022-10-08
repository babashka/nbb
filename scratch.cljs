(ns scratch)

(+ 1 2 3)

(require '[honey.sql :as sql])

(sql/format {:select :* :from :foo})

(assoc :foo :bar)

(js/eval "1 + 2 + 3")
