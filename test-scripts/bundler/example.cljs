(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]))

(def log js/console.log)

(log (chalk/blue "hello"))
(log (another/cool-fn))

