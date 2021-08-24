(ns esm-test.script
  (:require ["csv-parse/lib/sync$default" :as csv-parse]
            ["fs" :as fs]
            ["path" :as path]
            ["shelljs$default" :as sh]
            ["term-size$default" :as term-size]
            [nbb.core :refer [*file*]]))

(prn (path/resolve "."))
(prn (term-size))

(println (count (str (fs/readFileSync *file*))))

(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))
