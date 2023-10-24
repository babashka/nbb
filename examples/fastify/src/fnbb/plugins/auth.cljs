(ns fnbb.plugins.auth
  (:require [applied-science.js-interop :as j]
            [fnbb.config :refer [config]]
            [fnbb.utils :refer [render]]
            [fnbb.users :as d]
            [fnbb.plugins.layout :refer [layout]]
            ["url" :refer [URL]]
            ["@fastify/cookie$default" :as cookie]
            ["fast-jwt$default" :refer [createSigner createVerifier]]
            ["bcrypt$default" :as bcrypt]))

;; password = (.hasSync bcrypt "qwerty123456" (.genSaltSync bcrypt 10))
(def users (d/make-users-db))
(d/add-user! users :user1 {:username "user1" :password "$2b$10$tsXmxjFIutqTO5K71lbvnuS.Fia5BPiG8d5KwSFjBN5XqWm6dwp5q"})
(d/add-user! users :user2 {:username "user2" :password "$2b$10$tsXmxjFIutqTO5K71lbvnuS.Fia5BPiG8d5KwSFjBN5XqWm6dwp5q"})

(def jwt-sign-sync (createSigner (j/lit {:key (:jwt-secret config)
                                         :algorithm "HS256"
                                         :expiresIn (* 3600 1000 24) ;; 1 day in ms
                                         :allowedIss "https://example.com"})))

(def jwt-verify-sync (createVerifier (j/lit {:key (:jwt-secret config)
                                             :algorithms ["HS256"]
                                             :allowedIss "babashka"})))

(def prefix-whitelist #js["/login" "/logout" "/public/"])

(defn prefix-whitelisted?
  [original-path]
  (let [url (new URL (str "https://example.com" original-path)) pathname (j/get url :pathname)]
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
  ([]
   (template nil))
  ([error]
   [:form {"hx-post" "/login" "hx-target" "#login-form" "hx-ext" "json-enc"}
    [:fieldset
     [:legend "Login"]
     (if error [:div.form-error error] nil)
     [:div [:input {:name "username" :type "text" :placeholder "username"}]]
     [:div [:input {:name "password" :type "password" :placeholder "password"}]]
     [:button {:type "submit"} "Login"]]]))

(defn hx-template
  ([]
   (hx-template nil))
  ([error]
   [:div#login-form (template error)]))


(defn handler-login-get
  [_ reply]
  (let [html (layout (hx-template) :title "Login")]
    (-> reply
        (.header "content-type" "text/html")
        (.status 200)
        (.send (render html)))))

(def handler-login-post-schema
  (j/lit {:body {:type "object"
                 :required ["username" "password"]
                 :properties {:username {:type "string"}
                              :password {:type "string"}}}}))

(defn handler-login-post
  [req reply]
  (let [username (j/get-in req [:body :username])
        password (j/get-in req [:body :password])]
    (if-let [user (d/find-user users username)]
      (if-let [_ (.compareSync bcrypt password (get user :password))]
        (let [claim (j/lit {:iss "babashka" :sub username})
              token (jwt-sign-sync claim)]
          (-> reply
              (.setCookie "token" token (j/lit {:path "/"
                                                :signed true
                                                :httpOnly true}))
              (.status 200)
              (.header "HX-Redirect" "/")
              (.redirect "/")))
        (let [html (layout (template "Incorrect password") :title "Login")]
          (-> reply
              (.header "content-type" "text/html")
              (.status 200)
              (.send (render html)))))
      (let [html (layout (template "User not found") :title "Login")]
        (-> reply
            (.header "content-type" "text/html")
            (.status 200)
            (.send (render html)))))))


(defn handler-logout
  [_ reply]
  (-> reply
      (.clearCookie "token" (j/lit {:path "/"
                                    :signed true
                                    :httpOnly true}))
      (.redirect "/login")
      (.send true)))

(defn register
  [server _ done]
  (.register server cookie #js{:secret (:cookie-secret config)})
  (.decorateRequest server "current-user" nil)
  (.addHook server "preHandler" pre-handler)
  (done))

