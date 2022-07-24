(ns nbb.impl.deps
  (:require
   ["child_process" :as cproc]
   ["crypto" :as crypto]
   ["fs" :as fs]
   ["os" :as os]))


(def default-nbb-path
  (str (os/homedir) "/.nbb"))


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
        deps-path (str nbb-path "/_deps/" deps-hash)
        jar-path (str deps-path "/nbb-deps.jar")
        unzipped-path (str deps-path "/nbb-deps")]
    (when-not (fs/existsSync unzipped-path)
      (fs/mkdirSync deps-path #js {:recursive true})
      (println "Downloading dependencies...")
      (cproc/execSync (str "bb --config nbb.edn uberjar " jar-path))
      (println "Extracting dependencies...")
      (cproc/execSync (str "bb -e '(fs/unzip \""
                           jar-path
                           "\" \""
                           unzipped-path
                           "\")'"))
      (println "Done."))
    unzipped-path))
