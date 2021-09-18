(ns script
  (:require ["csv-parse/lib/sync$default" :as csv-parse]
            ["fs" :as fs]
            ["path" :as path]
            ["shelljs$default" :as sh]
            ["term-size$default" :as term-size]
            ["zx$default" :as zx]
            ["zx$fs" :as zxfs]
            [nbb.core :refer [*file*]]))

(prn (path/resolve "."))

(prn (term-size))

(println (count (str (fs/readFileSync *file*))))

(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))

(prn (zxfs/existsSync *file*))

(zx/$ #js ["ls"])
