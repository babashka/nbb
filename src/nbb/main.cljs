(ns nbb.main
  (:require [nbb.impl.main :as main]))

(defn main []
  (main/main))

;; needed on node 14 which is used in CI
#_(js/process.on
 "unhandledRejection"
 (fn [err]
   (.error js/console (ex-message err))
   (set! (.-exitCode  js/process) 1)))
