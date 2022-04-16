(ns nbb.api
  (:require ["import-meta-resolve" :as r]
            ["module" :refer [createRequire]]
            ["path" :as path]
            ["url" :as url]
            [nbb.classpath :as cp]
            [nbb.core :as nbb]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(defn import-meta-resolve []
  )

(defn init-require [path]
  (let [require (create-require path)]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (swap! nbb/ctx assoc :resolve #(do
                                     (prn :> % path)
                                     (r/resolve % (str (url/pathToFileURL path)))))))

(defn loadFile [script]
  (let [script-path (path/resolve script)]
    (init-require script-path)
    (nbb/load-file script-path)))

(defn loadString [expr]
  (init-require (path/resolve "script.cljs"))
  (nbb/load-string expr))

(defn addClassPath [cp]
  (cp/add-classpath cp))

(defn getClassPath []
  (cp/get-classpath))

(defn version []
  (nbb/version))
