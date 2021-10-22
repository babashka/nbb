(ns example
  (:require ["dockerode$default" :as Docker]
            [promesa.core :as p]))

(def docker (Docker. #js {:socketPath "/var/run/docker.sock"}))

(p/let [[output container] (.run docker "ubuntu" #js ["bash" "-c" "uname -a"]
                                 js/process.stdout)
        _ (prn :statuscode (.-StatusCode output))
        _ (.remove container)]
  (println "Container removed"))

;; Output:

;; $ nbb example.cljs
;; Linux fb18a5c5cdf6 5.10.47-linuxkit #1 SMP Sat Jul 3 21:51:47 UTC 2021 x86_64 x86_64 x86_64 GNU/Linux
;; :statuscode 0
;; Container removed
