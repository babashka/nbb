(ns nbb.build
  (:require [babashka.fs :as fs]
            [babashka.classpath :as classpath]
            [babashka.tasks :refer [shell clojure]]
            [clojure.string :as str]
            [clojure.edn :as edn]))

(defn- feature-files
  []
  (filter fs/exists?
          (map (fn [d]
                 (fs/file d "nbb_features.edn"))
               (classpath/split-classpath (classpath/get-classpath)))))

(defn read-configs
  [files]
  (->> files
       (mapcat (comp edn/read-string slurp str))))

(defn- wrap-cmd [cmd]
  (let [files (feature-files)
        feature-configs (read-configs files)
        ;; Assume feature ./src/nbb_features.edn has a ./deps.edn
        feature-dirs (map (comp fs/parent fs/parent) files)
        cmd (if (seq files)
              (format "-Sdeps '%s' %s"
                      {:deps
                       (into {}
                             (map (fn [dir]
                                    [(symbol (str (fs/file-name dir) "/deps"))
                                     {:local/root (str dir)}])
                                  feature-dirs))}
                      cmd)
              cmd)]
    ;; TODO: Add back *test*
    (if (seq feature-configs)
      (apply str cmd
        (map (fn [m] (format " --config-merge '%s'" (pr-str (:shadow-config m))))
             feature-configs))
      cmd)))

(defn build
  "Build nbb_core.js using given clojure args and commandline args"
  [cmd args]
  (apply clojure (wrap-cmd cmd) args))

(defn release
  "Compiles release build."
  []
  (build "-M -m shadow.cljs.devtools.cli --force-spawn release modules"
         *command-line-args*)
  (spit "lib/nbb_core.js"
        (clojure.string/replace (slurp "lib/nbb_core.js") (re-pattern "self") "globalThis"))
  (spit "lib/nbb_main.js"
        (str "#!/usr/bin/env node\n\n" (slurp "lib/nbb_main.js")))
  (shell "chmod +x lib/nbb_main.js")
  (run! fs/delete (fs/glob "lib" "**.map")))
