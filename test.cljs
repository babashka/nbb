(ns foo
  (:require ["fs" :as fs]
            ["csv-parse/lib/sync.js" :default csv-parse]))

(println (str (.readFileSync fs "test.cljs")))

(prn :hello-the-end)

(prn (csv-parse "foo,bar"))
