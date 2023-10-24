(ns fnbb.server
  (:require ["fastify$default" :as fastify]
            ["fastify-plugin$default" :as fp]
            [nbb.core :refer [await]]
            [applied-science.js-interop :as j]
            [fnbb.config :refer [config]]
            [fnbb.plugins.statics :refer [register] :rename {register register-statics}]
            [fnbb.plugins.htmx :refer [register] :rename {register register-htmx}]
            [fnbb.plugins.auth :refer [register
                                       handler-login-get
                                       handler-login-post-schema
                                       handler-login-post
                                       handler-logout] :rename {register register-auth}]
            [fnbb.plugins.home :refer [handler-home]]))

(def server (fastify (j/lit {:logger (:logger config)
                             :ignoreTrailingSlash (:ignoreTrailingSlash config)})))

;; plugins
(await (.register server (fp register-statics)))
(await (.register server (fp register-htmx)))
(await (.register server (fp register-auth)))

;; routes
(.route server #js{:method "GET" :url "/" :handler handler-home})
(.route server #js{:method "GET" :url "/login" :handler handler-login-get})
(.route server #js{:method "POST" :url "/login" :handler handler-login-post :schema handler-login-post-schema})
(.route server #js{:method "GET" :url "/logout" :handler handler-logout})
