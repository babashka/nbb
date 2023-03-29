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
   [sci.core :as sci]
   [shadow.esm :as esm]))

(def create-require
  (or createRequire
      (fn [_script-path]
        (fn [_]
          (throw (js/Error. "createRequire is not defined, this is a no-op"))))))

(def initialized? (atom false))

(defn resolve-nbb-edn
  "Finds a local nbb.edn file and reads it. Returns nil if none found."
  [path]
  (if (and (fs/existsSync path)
           (.isDirectory (fs/lstatSync path)))
    (let [resolved (path/resolve path "nbb.edn")]
      (if (fs/existsSync resolved)
        resolved
        (let [parent (path/dirname path)]
          (when-not (= parent path)
            (recur parent)))))
    (let [parent (path/dirname path)]
      (when-not (= parent path)
        (recur parent)))))

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
                  (if-let [config-file (resolve-nbb-edn path)]
                    (let [config (edn/read-string (fs/readFileSync config-file "utf8"))]
                      (assoc opts :config config :config-dir (path/dirname config-file)))
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
                 (doseq [p paths]
                   (if (not (path/isAbsolute p))
                     (cp/add-classpath (if-let [config-dir (:config-dir opts)]
                                         (path/resolve config-dir p)
                                         p))
                     (cp/add-classpath p)))
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

(defn loadMain [main-path expr]
  (let [script (loop [cp-entries (cp/split-classpath (cp/get-classpath))]
                 (let [cp-entry (first cp-entries)
                       filename (str cp-entry "/" main-path ".cljs")]
                   (when cp-entry
                     (if (fs/existsSync filename)
                       (path/resolve filename)
                       (recur (rest cp-entries))))))]
    (sci/binding [sci/file script]
      (-> (initialize script nil)
          (.then
           (fn []
             (let [ sci-ns @sci/ns]
               (nbb/eval-string* expr {:ns sci-ns :file script}))))))))

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
