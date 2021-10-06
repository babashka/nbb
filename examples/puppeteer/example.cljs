(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let
                                 example/defp clojure.core/def}}}
  (:require
   ["puppeteer$default" :as puppeteer]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is async]]
   [promesa.core :as p]))

(def continue (atom nil))
(defn pause []
  (js/Promise. (fn [resolve]
                (reset! continue resolve))))

(deftest browser-test
  (async done
   (p/let [browser (.launch puppeteer #js {:headless false})
           page (.newPage browser)
           _ (.goto page "https://clojure.org")
           _ (-> (.screenshot page #js{:path "screenshot.png"})
                 (.catch #(js/console.log %)))
           content (.content page)
           ;; uncomment to save content to variable for inspection
           ;; _ (def c content)
           ;; uncomment to pause execution to inspect state in browser
           ;; _ (pause)
           ]
     (is (str/includes? content "clojure"))
     (.close browser)
     (done))))

(t/run-tests 'example)

;;;; Scratch

(comment

  (browser-test)
  ;; evaluate to continue after pause
  (@continue)
  ;; inspect content captured during test
  ;; (subs c 0 10)

  (defmacro defp
    "Define var when promise is resolved"
    [binding expr]
    `(-> ~expr (.then (fn [val]
                        (def ~binding val)))))

  (defp browser (.launch puppeteer #js {:headless false}))
  (defp page (.newPage browser))
  (.goto page "https://clojure.org")
  (.close browser)
  )
