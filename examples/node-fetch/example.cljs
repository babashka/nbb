(ns example
  (:require
    [clojure.string :refer [capitalize]]
    [promesa.core :as p]
    ["fs" :refer [writeFileSync]]
    ["node-fetch$default" :as fetch]
    ["uuid" :refer [v4] :rename {v4 uuid}]))

(defn parse-user
  [{:keys [email name]
    {:keys [username password]} :login}]
  {:id         (uuid)
   :username   username
   :password   password
   :email      email
   :full-name  (str (capitalize (:first name))
                    " "
                    (capitalize (:last name)))})

(p/let [response (fetch "https://randomuser.me/api/?results=10")
        response (.json response)
        results (:results (js->clj response :keywordize-keys true))]
  (writeFileSync
    "randomUsers.edn"
    (pr-str (mapv parse-user results))))
