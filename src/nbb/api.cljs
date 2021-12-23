(ns nbb.api
  (:require #_["module" :refer [createRequire]]
            #_["path" :as path]
            [nbb.classpath :as cp]
            [nbb.core :as nbb]))

(def create-require
  #_(or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(defn init-require [path]
  (let [require (create-require path)]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)))

(defn loadFile [script]
  #_(let [script-path (path/resolve script)]
    (init-require script-path)
    (nbb/load-file script-path)))

(defn loadString [expr]
  #_(init-require (path/resolve "script.cljs"))
  (nbb/load-string expr))

(defn addClassPath [cp]
  (cp/add-classpath cp))

(defn getClassPath []
  (cp/get-classpath))

(defn version []
  (nbb/version))
