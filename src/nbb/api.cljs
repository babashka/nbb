(ns nbb.api
  (:require ["module" :refer [createRequire]]
            ["path" :as path]
            ["url" :as url]
            [nbb.classpath :as cp]
            [nbb.core :as nbb]
            [shadow.esm :refer [dynamic-import]])
  (:require-macros [nbb.macros :as macros]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(def imr (volatile! nil))

(defn lazy-resolve
  "Lazily loads import-meta-resolve. Needed as fallback for
  createRequire until import.meta.resolve becomes part of Node.js
  proper. Lazy loading shaves off about 15-30 ms of startup time, when not used."
  [lib path-url]
  (js/Promise.resolve (-> (or (some-> @imr js/Promise.resolve)
                              (-> (dynamic-import "import-meta-resolve")
                                  (.then (fn [mod]
                                           (vreset! imr (.-resolve mod))))))
                          (.then (fn [resolve]
                                   (resolve lib path-url))))))

(defn init-require [path]
  (let [require (create-require path)
        path-url (str (url/pathToFileURL path))]
    (set! (.-require goog/global) require)
    (swap! nbb/ctx assoc :require require)
    (swap! nbb/ctx assoc :resolve #(lazy-resolve % path-url))))

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

(defn registerModule [mod libname]
  (let [internal (nbb/libname->internal-name libname)]
    (nbb/register-module mod internal)))
