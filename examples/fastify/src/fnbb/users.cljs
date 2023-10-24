(ns fnbb.users)

(defn make-users-db
  []
  (atom {}))

(defn add-user!
  [users username user]
  (swap! users assoc username user))

(defn find-user
  [users username]
  (get @users (keyword username)))
