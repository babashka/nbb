(ns foo
  (:require ["fs" :as fs]
            ["csv-parse/lib/sync.js" :default csv-parse]
            [reagent.core :as r]
            #_[reagent.dom.server :as rds]))

(println (str (.readFileSync fs "test.cljs")))

(prn :hello-the-end)

(prn (csv-parse "foo,bar"))

#_(prn (rds/render-to-string [:div [:p "hello"]]))
