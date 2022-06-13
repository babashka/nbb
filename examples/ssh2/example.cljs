(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let}}}
  (:require
   ["fs" :as fs]
   ["ssh2" :refer [Client]]
   ["ssh2-promise$default" :as SSH2Promise]
   [promesa.core :as p]))

(def ssh-config
  #js {:host "yourhost"
       :port 22
       :username "username"
       :privateKey (fs/readFileSync "/home/user/.ssh/id_rsa")})

;; callback based:
(defn ssh-exec [command]
  (let [client (Client.)
        conn (.connect client ssh-config)]
    (-> conn
        (.on "error"
             (fn [err]
               (js/console.log err)))
        (.on "ready"
             (fn [] (println "stream ready")
               (.exec conn command
                      (fn [err stream]
                        (if err
                          (js/console.log err)
                          (-> stream
                              (.on "close"
                                   (fn [code _signal]
                                     (println (str "STRM:: close :: " code))
                                     (.end conn)))
                              (.on "data" (fn [data] (println (str "STDOUT:: " data)))))))))))))
;; example call:
#_(ssh-exec "ls")

;; promise based:
(defn ssh-exec-promise [command]
  (p/let [conn (new SSH2Promise ssh-config)
          data (.exec conn command)]
    (println "DATA:" data)
    (.close conn)))

;; example call:
#_(ssh-exec-promise "ls")
