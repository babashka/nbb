(ns example
  (:require ["chalk$default" :as chalk]))

(def log js/console.log)

(log (chalk/blue "hello"))


