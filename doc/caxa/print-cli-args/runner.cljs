(ns runner
  (:require ["node:path" :as path]
            [nbb.classpath :refer [add-classpath]]
            [nbb.core :refer [*file*]]))

(def dirname (path/dirname *file*))

(add-classpath (path/resolve dirname "src"))

(require '[print-cli-args.core])

(apply print-cli-args.core/-main *command-line-args*)
