(ns example
  (:require ["cheerio$default" :as cheerio]))

(def $ (cheerio/load "<div><a></a></div>"))

(-> ($ "div")
    (.addClass "container"))

(-> ($ "a")
    (.attr "href" "https://clojure.org")
    (.text "Clojure"))

(prn (.html $))
;; "<html><head></head><body><div class=\"container\"><a href=\"https://clojure.org\">Clojure</a></div></body></html>"
