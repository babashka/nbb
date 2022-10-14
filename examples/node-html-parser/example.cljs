(ns example
  (:require
   ["node-html-parser" :as html :refer [parse]]
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]))

(def root (parse "<ul id=\"list\" ><li>Hello World</li></ul>"))

(defn html->hiccup [elt]
  (cond
    (instance? html/HTMLElement elt)
    (into (cond-> [(keyword (str/lower-case (str (.-tagName elt))))]
            (pos? (count (js/Object.values (.-attributes elt))))
            (conj (js->clj (.-attributes elt) :keywordize-keys true)))
          (map html->hiccup (.-childNodes elt)))
    (instance? html/TextNode elt)
    (.-text elt)
    :else
    (do
      (prn elt ((js/eval "(x) => typeof(x)" elt)))
      elt)))

(run! pprint (map html->hiccup (.-childNodes root)))
