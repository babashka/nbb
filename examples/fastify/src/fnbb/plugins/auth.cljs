(ns fnbb.plugins.auth
  (:require [applied-science.js-interop :as j]
            [fnbb.config :refer [config]]
            [fnbb.plugins.layout :refer [layout]]
            [fnbb.utils :refer [render]]
            ["url" :refer [URL]]
            ["@fastify/cookie$default" :as cookie]
            ["fast-jwt$default" :refer [createSigner createVerifier]]))

;; password = bcrypt.hashSync("qwerty123456", bcrypt.genSaltSync(10))
(def users (j/lit [{:username "user1"
                    :password "$2b$10$tsXmxjFIutqTO5K71lbvnuS.Fia5BPiG8d5KwSFjBN5XqWm6dwp5q"
                    :roles []}
                   {:username "editor1"
                    :password "$2b$10$tsXmxjFIutqTO5K71lbvnuS.Fia5BPiG8d5KwSFjBN5XqWm6dwp5q"
                    :roles ["editor"]}
                   {:username "admin"
                    :password "$2b$10$tsXmxjFIutqTO5K71lbvnuS.Fia5BPiG8d5KwSFjBN5XqWm6dwp5q"
                    :roles ["admin"]}]))

(def jwt-sign-sync (createSigner (j/lit {:key (:jwt-secret config)
                                         :algorithm "HS256"
                                         :expiresIn (* 3600 1000 24) ;; 1 day in ms
                                         })))

(def jwt-verify-sync (createVerifier (j/lit {:key (:jwt-secret config)
                                             :algorithms ["HS256"]
                                             :allowedIss "babashka"})))

(def prefix-whitelist #js["/login" "/logout" "/public/"])

(defn prefix-whitelisted?
  [original-path]
  (let [url (new URL (str "http://example.com" original-path)) pathname (j/get url :pathname)]
    (if (.find prefix-whitelist (fn [prefix]
                                  (.startsWith pathname prefix)))
      true
      false)))

(defn pre-handler
  [req reply done]
  (if (prefix-whitelisted? (j/get req :url))
    :default
    (let [cookies (j/get req :cookies)]
      (if-let [signed-cookie (j/get cookies :token)]
        (let [unsigned-cookie (.unsignCookie req signed-cookie)]
          (if (j/get unsigned-cookie :valid)
            (if-let [token (jwt-verify-sync (j/get unsigned-cookie :value))]
              (println "--->>>" token)
              (-> reply
                  (.redirect "/login")
                  (.status 401)))
            (-> reply
                (.redirect "/login")
                (.status 401))))
        (-> reply
            (.redirect "/login")
            (.status 401)))))
  (done))

(defn template
  []
  [:fieldset
   [:legend "Login"]
   [:form {"hx-post" "/login" "hx-ext" "json-enc"}
    [:div [:input {:name "username" :type "text" :placeholder "username"}]]
    [:div [:input {:name "password" :type "password" :placeholder "password"}]]
    [:button {:type "submit"} "Login"]]])

(defn handler-login-get
  [_ reply]
  (let [html (layout (template) :title "Login")]
    (-> reply
        (.header "content-type" "text/html")
        (.status 200)
        (.send (render html)))))

(defn handler-login-post
  [req reply]
  (println (j/get req :body))
  (.send reply "ok"))


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

