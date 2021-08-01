(ns script
  #_"This is a smoke test, we have to turn this into real tests."
  (:require
   ["csv-parse/lib/sync" :as csv-parse]
   ["fs" :as fs]
   ["shelljs" :as sh]
   [clojure.string :as str]))

(println (count (str (.readFileSync fs "script.cljs"))))
(println (count (str (fs/readFileSync "script.cljs"))))
(prn (.ls sh "."))
(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))

(require '[reagent.core :as rg])
(prn (some? rg/as-element))
(prn (str/includes? "foo" "o"))
