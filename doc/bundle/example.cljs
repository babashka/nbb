(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   [from-classpath :as fc]
   [promesa.core :as p]
   [utils :as u]))

(def log js/console.log)

(log (chalk/blue "hello"))
(prn (fc/from-classpath))
(prn (another/cool-fn))

(p/-> (p/delay 1000 (u/util-fn))
      prn)
