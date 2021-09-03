(ns nbb.api
  (:require ["module" :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]))

(defn loadFile [script]
  (let [script-path (path/resolve script)
        require (createRequire script-path)]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (nbb/load-file script-path)))

(defn loadString [expr]
  (let [require (createRequire (path/resolve "script.cljs"))]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (nbb/load-string expr)))

