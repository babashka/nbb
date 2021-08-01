(ns script
  (:require
   ["csv-parse/lib/sync" :as csv-parse]
   ["fs" :as fs]
   ["shelljs" :as sh]))

(println (count (str (.readFileSync fs "script.cljs"))))
(println (count (str (fs/readFileSync "script.cljs"))))
(prn (.ls sh "."))
(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))

(require '[reagent.core :as rg])
(prn (some? rg/as-element))
