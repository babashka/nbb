(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   #_[promesa.core :as p]))

(def log js/console.log)

(log (chalk/blue "hello"))
(prn (another/cool-fn))

#_(p/-> (p/delay 1000 :hello)
      prn)
