(ns nbb.api
  (:require ["module" :refer [createRequire]]
            ["path" :as path]
            [nbb.core :as nbb]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(defn init-require [path]
  (let [require (create-require path)]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)))

(defn loadFile [script]
  (let [script-path (path/resolve script)]
    (init-require script)
    (nbb/load-file script-path)))

(defn loadString [expr]
  (init-require (path/resolve "script.cljs"))
  (nbb/load-string expr))
