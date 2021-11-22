(ns example
  (:require
   ["react-dom/server" :as ReactDomServer]
   [reagent.core :as r]))

(def page
  [:html
   [:body
    [:h1 "hello"]]])

(prn (.renderToStaticMarkup ReactDomServer (r/as-element page)))

;; Outputs
;; <html><body><h1>hello</h1></body></html>
