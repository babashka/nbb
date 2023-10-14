(ns fnbb.plugins.rbac
  (:require
   [nbb.core :refer [await slurp]]
   [applied-science.js-interop :as j]
   ["casbin$default" :refer [newEnforcer, newModel, StringAdapter]]))

(def model-txt (await (slurp "src/fnbb/plugins/rbac-model.txt")))

(def adapter-txt (await (slurp "src/fnbb/plugins/rbac-adapter.txt")))

(def model (newModel model-txt))

(def adapter (new StringAdapter adapter-txt))

(def enforcer (await (newEnforcer model adapter)))

(defn register
  [server _ done]
  (.decorateRequest server "enforcer" nil)
  (.addHook server "preHandler" (fn [req _ done]
                                  (j/assoc! req :enforcer enforcer)
                                  (done)))
  (done))
