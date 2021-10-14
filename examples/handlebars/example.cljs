(ns example
  (:require ["handlebars$default" :as handlebars]))

(def template (.compile handlebars "Hello {{name}}!"))

(prn (template #js {:name "world"}))
