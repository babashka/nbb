#!/usr/bin/env nbb

(ns birch
  "Birch code adapted from https://github.com/lambdaisland/birch/blob/master/src/birch/core.cljs.")

(def node-fs (js/require "node:fs"))
(def node-path (js/require "node:path"))

(def read-dir (.-readdirSync node-fs))
(def stat (.-statSync node-fs))
(def path-join (.-join node-path))

(defn directory? [f]
  (.isDirectory (stat f)))

(def I-branch "│   ")
(def T-branch "├── ")
(def L-branch "└── ")
(def SPACER   "    ")

(declare tree-entry)

(defn child-entries [path]
  (map #(tree-entry path %1) (read-dir path)))

(defn tree-entry [parent name]
  (try
    (let [path (path-join parent name)
          is-dir (directory? path)]
      {:name name
       :directory? is-dir
       :children (when is-dir (child-entries path))})
    (catch :default _ nil)))

(defn render-tree [{:keys [name children]}]
  (cons
   name
   (mapcat (fn [child index]
             (let [subtree (render-tree child)
                   last? (= index (dec (count children)))
                   prefix-first (if last? L-branch T-branch)
                   prefix-rest  (if last? SPACER I-branch)]
               (cons (str prefix-first (first subtree))
                     (map #(str prefix-rest %) (next subtree)))))
           children
           (range))))

(let [[_ _ _ dir] js/process.argv]
  (->> (tree-entry "" dir)
       (render-tree)
       (run! println)))
