(ns fastify-nbb.plugins.auth
  (:require [applied-science.js-interop :as j]
            [fastify-nbb.config :refer [config]]
            [fastify-nbb.plugins.layout :refer [layout]]
            [fastify-nbb.utils :refer [render]]
            ["url" :refer [URL]]
            ["@fastify/cookie$default" :as cookie]
            ["fast-jwt$default"
             :refer [createSigner createVerifier]
             :rename {createSigner create-jwt-signer createVerifier create-jwt-verifier}]))

(def jwt-sign-sync (create-jwt-signer (j/lit {:key (:jwt-secret config)
                                              :algorithm "HS256"
                                              :expiresIn (* 3600 1000 24) ;; 1 day in ms
                                              })))

(def jwt-verify-sync (create-jwt-verifier (j/lit {:key (:jwt-secret config)
                                                  :algorithms ["HS256"]
                                                  :allowedIss "babashka"})))

(def pathname-whitelist #{"/login" "/logout"})

(defn pathname-whitelisted?
  [pathname]
  (let [url (new URL (str "http://example.com" pathname))]
    (contains? pathname-whitelist (j/get url :pathname))))

(defn pre-handler
  [req _ done]
  (if (pathname-whitelisted? (j/get req :url))
    :default
    (let [cookies (j/get req :cookies)]
      (if-let [signed-cookie (j/get cookies :token)]
        (let [unsigned-cookie (.unsignCookie req signed-cookie)]
          (if (j/get unsigned-cookie :valid)
            (if-let [token (jwt-verify-sync (j/get unsigned-cookie :value))]
              (println "--->>>" token)
              (throw (js/Error "ERR_UNAUTHORIZED")))
            (throw (js/Error "ERR_UNAUTHORIZED"))))
        (throw (js/Error "ERR_UNAUTHORIZED")))))
  (done))

(defn template
  []
  [:form {"hx-post" "/login" "hx-ext" "json-enc"}
   [:div [:input {:name "username" :type "text" :placeholder "username"}]]
   [:div [:input {:name "password" :type "password" :placeholder "password"}]]
   [:button {:type "submit"} "Login"]])

(defn handler-login
  [req reply]

  (if (= (j/get req :method) "GET")
    (let [html (layout (template) :title "Home")]
      (-> reply
          (.header "content-type" "text/html")
          (.status 200)
          (.send (render html))))
    (js/JSON.stringify (j/get req :body))))

(defn handler-logout
  [_ reply]
  (-> reply
      (.clearCookie)))

(defn register
  [server _ done]
  (.register server cookie #js{:secret (:cookie_secret config)})
  (.decorateRequest server "current-user" nil)
  (.addHook server "preHandler" pre-handler)
  (done))

