(ns script
  (:require ["./foo.mjs$default" :as foo]))

(defn -main [& _]
  (js->clj foo :keywordize-keys true))
