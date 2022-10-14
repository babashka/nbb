(ns example
  (:require
   ["node-html-parser" :as html :refer [parse]]
   [clojure.string :as str]
   [clojure.walk :as walk]))

(def root (parse "<ul id=\"list\" ><li>Hello World</li></ul>"))

(defn html->hiccup [root]
  (walk/prewalk
   (fn [elt]
     (cond
       (instance? html/HTMLElement elt)
       (into (cond-> [(keyword (str/lower-case (str (.-tagName elt))))]
               (pos? (count (js/Object.values (.-attributes elt))))
               (conj (js->clj (.-attributes elt) :keywordize-keys true))) (.-childNodes elt))
       (instance? html/TextNode elt)
       (.-text elt)
       :else
       elt))
   (.-firstChild root)))

(prn (html->hiccup root))
