(ns example
  (:require
   ["posthtml-parser" :refer [parser]]
   [clojure.pprint :refer [pprint]]
   [hickory.select :as hs]))

(defn parse [s]
  (js->clj (parser s) :keywordize-keys true))

(defn html->hiccup [{:keys [tag attrs content] :as elt}]
  (if tag
    (into (cond-> [(keyword tag)]
            (seq attrs) (conj attrs))
          (map html->hiccup content))
    elt))

(def parsed (parse "<ul id=\"list\">
               <li class=\"item\">Hello</li>
               <li class=\"item\">Goodbye</li>
             </ul>"))

(pprint
 (-> parsed first html->hiccup))

;; [:ul
;;  {:id "list"}
;;  "\n               "
;;  [:li {:class "item"} "Hello"]
;;  "\n               "
;;  [:li {:class "item"} "Goodbye"]
;;  "\n             "]

(prn
 (->> parsed
     first
     (hs/select (hs/attr :class #(= "item" %)))
     (mapv :content)))

;; [["Hello"] ["Goodbye"]]
