(ns example
  (:require
   ["posthtml-parser" :refer [parser]]
   [clojure.pprint :refer [pprint]]))

(defn parse [s]
  (js->clj (parser s) :keywordize-keys true))

(defn html->hiccup [{:keys [tag attrs content] :as elt}]
  (if tag
    (into (cond-> [(keyword tag)]
            (seq attrs) (conj attrs))
          (map html->hiccup content))
    elt))

(run! (comp pprint html->hiccup) (parse "<ul id=\"list\" ><li>Hello World</li></ul>"))
