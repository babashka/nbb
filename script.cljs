(ns script
  (:require
   ["csv-parse/lib/sync" :as csv-parse]
   ["fs" :as fs]
   ["shelljs" :as sh]))

(println (count (str (.readFileSync fs "script.cljs"))))

(prn (.ls sh "."))

(prn (csv-parse "foo,bar"))

;; evaluate local file
(js/eval (str (.readFileSync fs "./foo.js")))

(def fs2 (js/require "fs"))
(prn (some? fs))
(println (count (str (.readFileSync fs2 "script.cljs"))))

