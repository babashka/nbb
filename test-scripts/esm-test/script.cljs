(ns script
  (:require ["csv-parse/lib/sync$default" :as csv-parse]
            ["node:fs" :as fs]
            ["node:path" :as path]
            ["shelljs$default" :as sh]
            ["term-size$default" :as term-size]
            ;; This is an important test, do not change:
            ;; load zx without props and with props:
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

(def term-size' (await (js/import "term-size")))
(prn (term-size'.default))

(def js-file (await (js/import "./test-js-file.mjs")))
(assert (= 10 (.-x js-file)))
