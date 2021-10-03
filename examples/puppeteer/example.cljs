(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let}}}
  (:require
   ["puppeteer$default" :as puppeteer]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is async]]
   [promesa.core :as p]))

(deftest browser-test
  (async done
   (p/let [browser (.launch puppeteer)
           page (.newPage browser)
           _ (.goto page "https://clojure.org")
           _ (-> (.screenshot page #js{:path "screenshot.png"})
                 (.catch #(js/console.log %)))
           content (.content page)]
     (is (str/includes? content "clojure"))
     (.close browser)
     (done))))

(t/run-tests 'example)
