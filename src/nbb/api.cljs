(ns nbb.api
  (:require
   ["fs" :as fs]
   ["import-meta-resolve" :as imr]
   ["module" :refer [createRequire]]
   ["path" :as path]
   ["url" :as url]
   [clojure.edn :as edn]
   [nbb.classpath :as cp]
   [nbb.core :as nbb]
   [shadow.esm :as esm]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(def initialized? (atom false))

(defn local-nbb-edn
  "Finds a local nbb.edn file and reads it. Returns nil if none found."
  []
  (when (fs/existsSync "nbb.edn")
    (edn/read-string (fs/readFileSync "nbb.edn" "utf8"))))

(defn initialize [path opts]
  (js/Promise.resolve
   (when-not @initialized?
     ;; (prn :path path)
     (let [path (path/resolve (or path "script.cljs"))
           opts (if (:config opts)
                  (assoc opts :config
                         (edn/read-string
                          (fs/readFileSync
                           (:config opts)
                           "utf8")))
                  (if-let [config (local-nbb-edn)]
                    (assoc opts :config config)
                    opts))
           require (create-require path)
           path-url (str (url/pathToFileURL path))]
       (set! (.-require goog/global) require)
       (swap! nbb/ctx assoc :require require)
       (swap! nbb/ctx assoc :resolve #(imr/resolve % path-url))
       (reset! nbb/opts opts)
       (->
        (js/Promise.resolve
         (if-let [config (:config opts)]
           (do (if-let [paths (:paths config)]
                 (doseq [path paths]
                   (cp/add-classpath path))
                 ;; default classpath
                 (cp/add-classpath (js/process.cwd)))
               (esm/dynamic-import "./nbb_deps.js"))
           ;; default classpath
           (cp/add-classpath (js/process.cwd))))
        (.then (fn [_]
                 (reset! initialized? true))))))))

(defn loadFile [script]
  (let [script-path (path/resolve script)]
    (-> (initialize script-path nil)
        (.then #(nbb/load-file script-path)))))

(defn loadString [expr]
  (-> (initialize nil nil)
      (.then
       #(nbb/load-string expr))))

(defn addClassPath [cp]
  (cp/add-classpath cp))

(defn getClassPath []
  (cp/get-classpath))

(defn version []
  (nbb/version))

(defn registerModule [mod libname]
  (let [internal (nbb/libname->internal-name libname)]
    (nbb/register-module mod internal)))

(def printErrorReport nbb/print-error-report)
