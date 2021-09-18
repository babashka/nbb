(ns script
  (:require ["chalk$default" :as chalk]
            ["console$log" :as log]))

;; Combine styled and normal strings
(log (chalk/blue "hello") (str " world" (chalk/red "!")))

;; Compose multiple styles using the chainable API
(log (chalk/blue.bgRed.bold "Hello world!"))

;; Pass in multiple arguments
(log (chalk/blue "Hello" "World!" "Foo" "bar" "biz" "baz"))

;; Nest styles
(log (chalk/red "Hello" (str (chalk/underline.bgBlue "world") "!")))
