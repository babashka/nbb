(ns fastify-nbb.server
  (:require ["fastify$default" :as fastify]
            ["fastify-plugin$default" :as fp]
            [nbb.core :refer [await]]
            [applied-science.js-interop :as j]
            [fastify-nbb.config :refer [config]]
            [fastify-nbb.plugins.htmx :refer [register] :rename {register register-htmx}]
            [fastify-nbb.plugins.rbac :refer [register] :rename {register register-rbac}]
            [fastify-nbb.plugins.auth :refer [register handler-login handler-logout] :rename {register register-auth}]
            [fastify-nbb.plugins.home :refer [handler-home]]))

(def server (fastify #js {:logger (:logger config)
                          :ignoreTrailingSlash (:ignoreTrailingSlash config)}))

;; plugins
(await (.register server (fp register-htmx)))
(await (.register server (fp register-rbac)))
(await (.register server (fp register-auth)))

;; routes
(.route server #js {:method "GET" :url "/" :handler handler-home})
(.route server (j/lit {:method ["GET" "POST"] :url "/login" :handler handler-login}))
(.route server #js {:method "GET" :url "/logout" :handler handler-logout})
