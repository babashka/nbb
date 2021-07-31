(ns script
  (:require
   ["csv-parse/lib/sync.js" :default csv-parse]
   ["fs" :as fs]
   ["shelljs" :default sh]))

(println (count (str (.readFileSync fs "script.cljs"))))

(prn (.ls sh "."))

(prn (csv-parse "foo,bar"))

;; evaluate local file
(js/eval (str (.readFileSync fs "./foo.js")))
