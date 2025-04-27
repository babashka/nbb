(ns nbb.main
  (:require
   ["node:process" :as process]
   [nbb.impl.main :as main]))

(defn main []
  (main/main))

;; needed on node 14 which is used in CI
(when process/on
  (process/on
   "unhandledRejection"
   (fn [err]
     (.error js/console (ex-message err))
     (set! (.-exitCode  js/process) 1))))
