(ns nbb.build
  "Provides bb tasks for building and releasing nbb"
  (:require
   [babashka.classpath :as classpath]
   [babashka.fs :as fs]
   [babashka.tasks :refer [clojure shell]]
   [clojure.edn :as edn]
   [clojure.string :as str]))

(defn- feature-files
  []
  (filter fs/exists?
          (map (fn [d]
                 (fs/file d "nbb_features.edn"))
               (classpath/split-classpath (classpath/get-classpath)))))

(defn- read-configs
  [files]
  (->> files
       (mapcat (comp edn/read-string slurp str))))

(defn- build-cmd [cmd nbb-dir]
  (let [files (feature-files)
        feature-configs (read-configs files)
        ;; Each ./src/nbb_features.edn has a ./deps.edn
        feature-dirs (map (comp fs/parent fs/parent) files)
        cmd' (if (seq files)
               (format "-Sdeps '%s' %s"
                       {:deps
                        (merge (into {}
                                     (map (fn [dir]
                                            [(symbol (str (fs/file-name dir) "/deps"))
                                             {:local/root (str dir)}])
                                          feature-dirs))
                               {'nbb/deps {:local/root nbb-dir}})}
                       cmd)
               cmd)]
    (when (seq feature-configs)
      (println "Building features:" (str/join ", " (map :name feature-configs)) "..."))
    (if (seq feature-configs)
      (apply str cmd'
        (map (fn [m] (format " --config-merge '%s'" (pr-str (:shadow-config m))))
             feature-configs))
      cmd')))

(defn build
  "Build nbb shadow builds using clojure cmd and commandline args. Features on
  classpath are automatically added"
  [cmd args]
  (let [building-outside-nbb? (not (fs/exists? "shadow-cljs.edn"))
        nbb-dir (when building-outside-nbb?
                  (->> (classpath/get-classpath)
                       classpath/split-classpath
                       ;; Pull out nbb from local/root or git/url
                       (some #(when (re-find #"(nbb/[0-9a-f]+|nbb)/src" %) %))
                       fs/parent))]
    (when building-outside-nbb?
      (fs/copy (fs/file nbb-dir "shadow-cljs.edn") "shadow-cljs.edn"))
    (apply clojure (build-cmd cmd (str nbb-dir)) args)
    (when building-outside-nbb?
      (fs/delete "shadow-cljs.edn"))))

(defn move-ext-lib [src dest]
  (fs/move src dest {:replace-existing true})
  (spit dest
        (-> (slurp dest)
            (str/replace "from \"./nbb_core.js\";" "from \"nbb/lib/nbb_core.js\";")
            (str/replace "import  \"./nbb_goog_string.js\";" "import \"nbb/lib/nbb_goog_string.js\";"))))

(defn release
  "Compiles release build."
  [args & {:keys [wrap-cmd-fn] :or {wrap-cmd-fn identity}}]
  (build (wrap-cmd-fn "-M -m shadow.cljs.devtools.cli --force-spawn release modules")
         args)
  (spit "lib/nbb_core.js"
        (str/replace (slurp "lib/nbb_core.js") (re-pattern "self") "globalThis"))
  (run! fs/delete (fs/glob "lib" "**.map"))
  #_(move-ext-lib "lib/nbb_schema.js" "ext/nbb-prismatic-schema/index.mjs")
  #_(move-ext-lib "lib/nbb_malli.js" "ext/nbb-metosin-malli/index.mjs"))
