(ns script
  (:require ["csv-parse/lib/sync$default" :as csv-parse]
            ["fs" :as fs]
            ["path" :as path]
            ["shelljs$default" :as sh]
            ["term-size$default" :as term-size]
            ["zx" :as zx]
            ["zx$fs" :as zxfs]
            ["execa" :as execa]
            [nbb.core :refer [*file* await]]))

(prn (path/resolve "."))

(prn (term-size))

(println (count (str (fs/readFileSync *file*))))

(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))

(prn (zxfs/existsSync *file*))

(await (zx/$ #js ["ls"]))

(prn (execa/execaSync "ls"))
