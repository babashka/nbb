(ns example
  (:require
   ["chalk$default" :as chalk]
   [another-namespace :as another]
   [from-classpath :as fc]
   [goog.string :as gstring]
   [promesa.core :as p]
   [clojure.edn :as edn]
   [utils :as u]))

(def log js/console.log)

(log (chalk/blue "hello"))
(prn (fc/from-classpath))
(prn (another/cool-fn))
(prn (gstring/format "Awesome formatted: (%s)" "Yes!"))
(p/-> (p/delay 1000 (u/util-fn))
      prn)
(prn (edn/read-string "[1 2 3]"))
