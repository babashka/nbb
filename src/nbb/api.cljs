(ns nbb.api
  (:require ["module" :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(defn loadFile [script]
  (let [script-path (path/resolve script)
        require (create-require script-path)]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (nbb/load-file script-path)))

(defn loadString [expr]
  (let [require (create-require (path/resolve "script.cljs"))]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (nbb/load-string expr)))

