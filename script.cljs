(ns script
  "This is a smoke test, we have to turn this into real tests."
  (:require
   ["csv-parse/lib/sync" :as csv-parse]
   ["fs" :as fs]
   ["shelljs" :as sh :refer [ls]]
   [clojure.string :as str]))

;; imports + interop
(println (count (str (.readFileSync fs "script.cljs"))))
(println (count (str (fs/readFileSync "script.cljs"))))
(prn (.ls sh "."))
(prn (sh/ls "."))
(prn (ls "."))
(prn (csv-parse "foo,bar"))

;; optional lazy-loaded module
(require '[reagent.core :as rg])
(prn (some? rg/as-element))

;; built-in namespaces
(prn (str/includes? "foo" "o"))
(prn *command-line-args*)
