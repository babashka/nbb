(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   [promesa.core :as p]))

(def log js/console.log)

(log (chalk/blue "hello"))
(log (another/cool-fn))

(p/-> (p/delay 1000 :hello)
      prn)
