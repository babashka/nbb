(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let}}}
  (:require ["puppeteer" :as puppeteer]
            [promesa.core :as p]))

;; This async code is much nicer with p/let:
#_(-> (.launch puppeteer)
      (.then (fn [browser]
               (-> (.newPage browser)
                   (.then (fn [page]
                            (-> (.goto page "https://clojure.org")
                                (.then #(.screenshot page #js{:path "screenshot.png"}))
                                (.catch #(js/console.log %))
                                (.then #(.close browser)))))))))

(p/let [browser (.launch puppeteer)
        page (.newPage browser)
        _ (.goto page "https://clojure.org")
        _ (-> (.screenshot page #js{:path "screenshot.png"})
              (.catch #(js/console.log %)))]
  (.close browser))
