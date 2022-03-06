(ns example
  (:require ["execa" :refer [execa]]
            [applied-science.js-interop :as j]
            [promesa.core :as p]))

;; see https://blog.logrocket.com/running-commands-with-execa-in-node-js/

(p/let [{:keys [stdout]} (p/-> (execa "echo" #js ["execa is pretty great"])
                               j/lookup)]
  (prn :stdout stdout)) ;; :stdout "execa is pretty great"
