(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   [promesa.core :as p]
   [utils :as u]))

(def log js/console.log)

(log (chalk/blue "hello"))
(prn (another/cool-fn))

(p/-> (p/delay 1000 (u/util-fn))
      prn)
