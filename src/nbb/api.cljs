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
         (if-let [config (and (not (:disableConfig opts)) (:config opts))]
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

(defn loadFile
  ([script] (loadFile script nil))
  ([script opts]
   (let [opts (js->clj opts :keywordize-keys true)
         script-path (path/resolve script)]
     (reset! nbb/-invoked-file script-path)
     (-> (initialize script-path opts)
         (.then #(nbb/load-file script-path))))))

(defn loadString
  ([expr] (loadString expr nil))
  ([expr opts]
   (let [opts (js->clj opts :keywordize-keys true)]
     (-> (initialize nil opts)
         (.then
          #(nbb/load-string expr))))))

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
