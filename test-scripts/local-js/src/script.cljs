(ns script
  (:require
   ["./foo.mjs$default" :as foo]
   [other.script :as o]))

(defn -main [& _]
  (assoc (js->clj foo :keywordize-keys true)
         :other.script/foo o/foo))
