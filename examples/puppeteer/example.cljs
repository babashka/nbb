(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let
                                 example/defp clojure.core/def}}}
  (:require
   ["puppeteer$default" :as puppeteer]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is async]]
   [promesa.core :as p]))

(defn sleep [ms]
  (js/Promise.
   (fn [resolve]
     (js/setTimeout. #(resolve) ms))))

(deftest browser-test
  (async done
   (p/let [browser (.launch puppeteer #js {:headless false})
           page (.newPage browser)
           _ (.goto page "https://clojure.org")
           _ (-> (.screenshot page #js{:path "screenshot.png"})
                 (.catch #(js/console.log %)))
           content (.content page)
           _ (sleep 1000)]
     (is (str/includes? content "clojure"))
     (.close browser)
     (done))))

(t/run-tests 'example)

;;;; Scratch

(comment

  (defmacro defp [binding expr]
    `(-> ~expr (.then (fn [val]
                        (def ~binding val)
                        val))))
  (p/do!

   ;; eval these one by one or the entire p/do! at once to await each successive step
   ;; and inspect the variables
   
   (defp browser (.launch puppeteer #js {:headless false}))
   (defp page (.newPage browser))
   (.goto page "https://www.clojure.org")
   (defp content (.content page))
   content
   (.close browser))
  
  ,)
