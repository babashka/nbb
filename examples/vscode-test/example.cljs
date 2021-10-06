(ns example
  (:require ["vscode-test" :refer [downloadAndUnzipVSCode]]
            [promesa.core :as p]))

(p/let [_ (downloadAndUnzipVSCode "1.36.1")]
  )
