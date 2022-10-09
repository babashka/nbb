(ns script
  (:require
   ["./async_require.mjs" :refer [res1 res2 res3 res4]]
   ["./foo.mjs$default" :as foo]
   [nbb.core :refer [await]]
   [promesa.core :as p]))

(def p (require '[other.script :as o]))

(await (p/delay 50))
(await p)

(defn -main [& _]
  (assoc (js->clj foo :keywordize-keys true)
         :other.script/foo o/foo
         :nss [res1 res2 res3 res4]))
