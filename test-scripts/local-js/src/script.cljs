(ns script
  (:require
   ["./async_require.js" :refer [res1 res2 res3 res4]]
   ["./foo.mjs$default" :as foo]
   [other.script :as o]))

(defn -main [& _]
  (assoc (js->clj foo :keywordize-keys true)
         :other.script/foo o/foo
         :nss [res1 res2 res3 res4]))
