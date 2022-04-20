(ns example
  (:require ["net" :as net]))

(def port 1337)

(def server (net/createServer
             (fn [sock]
               (.on sock "data"
                    (fn [data]
                      ;; print data to console
                      (prn :data (str data))
                      ;; write response back to client
                      (.write sock (str "You wrote: " data)))))))

(.listen server port "localhost")

;; $ nc localhost 1337
;; 1
;; You wrote: 1
;; 2
;; You wrote: 2
