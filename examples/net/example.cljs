(ns example
  (:require ["net" :as net]))

(def port 1337)

(def server (net/createServer
             (fn [sock]
               (.on sock "data"
                    (fn [data]
                      ;; print data to console
                      (println (str "[server] Client sent: " data))
                      ;; write response back to client
                      (.write sock (str "You wrote: " data))))
               (.on sock "end"
                    (fn []
                      (println "[server] Client left the building"))))))

(.listen server port "localhost")

(def client (net/createConnection #js {:port port}
                                  (fn [] (println "[client] Connected to server!"))))
(.write client "Hello")
(.on client "data"
     (fn [data]
       (println "[client] Received:" (str data))
       (.destroy client)))

;; [client] Connected to server!
;; [server] Client sent: Hello
;; [client] Received: You wrote: Hello
;; [server] Client left the building
