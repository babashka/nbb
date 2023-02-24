(ns nbb.impl.deps
  (:require
   ["child_process" :as cproc]
   ["crypto" :as crypto]
   ["fs" :as fs]
   ["path" :as path]
   [nbb.classpath :as cp]
   [nbb.core :as nbb :refer [opts]]))

(defn hash-deps
  "Given a map of dependencies, generates a unique hash of that map for
  caching purposes."
  [deps]
  (.. (crypto/createHash "sha1") (update (str deps) "binary") (digest "hex")))

(defn download-and-extract-deps!
  "Given a map of dependencies and a path, downloads all dependencies to
  '*nbb-path*/_deps/*hash-of-deps-map*/nbb-deps' and returns that full path."
  [deps nbb-path]
  (let [deps-hash (hash-deps deps)
        deps-path (path/resolve nbb-path deps-hash)
        deps-edn-path (path/resolve deps-path "deps.edn")
        jar-path (path/resolve deps-path "nbb-deps.jar")
        unzipped-path (path/resolve deps-path "nbb-deps")]
    (when-not (fs/existsSync unzipped-path)
      (let [bb (if (= "win32" js/process.platform)
                 "bb"
                 ;; this wasn't necessary, so let's not do it yet
                 #_"bb.exe"
                 "bb")
            extract-script (path/resolve nbb-path "extract.bb")]
        (fs/mkdirSync deps-path #js {:recursive true})
        (fs/writeFileSync deps-edn-path (str {:deps deps}))
        (*print-err-fn* "Downloading dependencies...")
        (cproc/execSync (str bb " --config " deps-edn-path " uberjar " jar-path))
        (*print-err-fn* "Extracting dependencies...")
        (fs/writeFileSync extract-script
                          (str "(fs/unzip "
                               (pr-str jar-path)
                               " "
                               (pr-str unzipped-path)
                               ")"))
        (cproc/execSync (str "bb " (str extract-script)))
        (fs/unlinkSync extract-script)
        (*print-err-fn* "Done.")))
    unzipped-path))

(defn init
  []
  (let [config-dir (get @opts :config-dir)
        cache-path (path/resolve config-dir ".nbb" ".cache")]
    (when-let [deps (get-in @opts [:config :deps])]
      (-> deps
          (download-and-extract-deps! cache-path)
          (cp/add-classpath)))))
