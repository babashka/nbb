(ns example
  (:require ["glob$default" :as glob]
            ["process" :refer [chdir]]))

(chdir "..")

(prn (glob/sync "**/**.cljs"))
