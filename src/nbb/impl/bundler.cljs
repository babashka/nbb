(ns nbb.impl.bundler
  "Mostly a copy of babashka uberscript, but generating an .mjs file for Node to execute"
  (:require
   ["node:fs" :as fs]
   ["esbuild" :as esbuild]
   [clojure.string :as str]
   [goog.string :as gstring]
   [goog.string.format]
   [nbb.core :as nbb :refer [opts]]
   [sci.core :as sci]
   [sci.ctx-store :as store]))

(defn decompose-clause [clause]
  ;;(prn :clause clause)
  (if (symbol? clause)
    {:ns clause}
    (when (seqable? clause)
      (let [clause (if (= 'quote (first clause))
                     (second clause)
                     clause)
            [ns & tail] clause]
        (loop [parsed {:ns ns}
               tail (seq tail)]
          (if tail
            (let [ftail (first tail)]
              (case ftail
                :as (recur (assoc parsed :as (second tail))
                           (nnext tail))
                :refer
                (let [refer (second tail)]
                  (if (seqable? refer)
                    (recur (assoc parsed :refer (second tail))
                           (nnext tail))
                    (recur parsed (nnext tail))))
                ;; default
                (recur parsed
                       (nnext tail))))
            parsed))))))

(defn recompose-clause [{:keys [:ns :as]}]
  [ns :as as])

(defn process-ns
  [_ctx ns]
  (keep (fn [x]
          (if (seqable? x)
            (let [fx (first x)]
              (when (= :require fx)
                (let [decomposed (keep decompose-clause (rest x))
                      recomposed (map recompose-clause decomposed)]
                  (list* :require recomposed))))
            x))
        ns))

(defn keep-quoted [clauses]
  (keep (fn [clause]
          (when (and (seq? clause) (= 'quote (first clause)))
            (second clause)))
        clauses))

(defn process-require [_ctx req]
  (let [quoted (keep-quoted (rest req))
        decomposed (map decompose-clause quoted)]
    (list* 'require (map (fn [clause]
                           (list 'quote (recompose-clause clause)))
                         decomposed))))

(defn process-in-ns [_ctx req]
  (let [quoted (keep-quoted (rest req))
        quoted (map (fn [ns]
                      (list 'quote ns))
                    quoted)]
    (when (clojure.core/seq quoted)
      (list* 'in-ns quoted))))

(defn loc [rdr]
  (str (when-let [f @sci/file]
         (str f ":"))
       (sci/get-line-number rdr)
       ":"
       (sci/get-column-number rdr)))

(defn uberscript [{:keys [ctx expressions]}]
  (let [ctx (assoc ctx :reload-all true)]
    (sci/binding [sci/file @sci/file
                  sci/ns @sci/ns
                  sci/print-fn identity]
      (doseq [expr expressions]
        (let [rdr (sci/reader expr)]
          ;;(prn :> (sci/parse-next ctx rdr))
          (loop []
            (let [next-val
                  (try (sci/parse-next ctx rdr {:features #{:cljs :org.babashka/nbb}})
                       ;; swallow reader error
                       (catch :default e
                         (js/console.log (ex-message e))
                         (js/console.error "[babashka]" "Ignoring read error while assembling uberscript near"
                                           (str (loc rdr)))))]
              (when-not (= ::sci/eof next-val)
                (if (seq? next-val)
                  (let [fst (first next-val)
                        expr (cond (= 'ns fst)
                                   (process-ns ctx next-val)
                                   (= 'require fst)
                                   (process-require ctx next-val)
                                   (= 'in-ns fst)
                                   (process-in-ns ctx next-val))]
                    (when expr
                      (try
                        ;;(prn :eval expr)
                        (sci/eval-form ctx expr)
                        ;; swallow exception and continue
                        (catch :default e
                          (js/console.log e)
                          (js/console.error "[babashka]" "Ignoring read error while assembling uberscript near"
                                            (str (loc rdr))))))
                    (recur))
                  (recur))))))))))

(defn build-executable [bundled-js output-file]
  (let [tmp-file (str "./.nbb-tmp-" (random-uuid) ".mjs")]
    (fs/writeFileSync tmp-file bundled-js)
    (-> (esbuild/build
          (clj->js {:entryPoints [tmp-file]
                    :bundle true
                    :platform "node"
                    :minify true
                    :format "esm"
                    :banner {:js "import { createRequire } from 'module';const require = createRequire(import.meta.url);"}
                    :write false}))
        (.then (fn [esbuild-result]
                 (let [output-file-obj (first (aget esbuild-result "outputFiles"))
                       esbuild-code (aget output-file-obj "contents")]
                   (fs/writeFileSync output-file
                                     "#!/usr/bin/env -S node --experimental-default-type=module\n")
                   (fs/appendFileSync output-file esbuild-code)
                   (fs/chmodSync output-file 0755)
                   (fs/unlinkSync tmp-file)
                   (println (str "Single file executable created: " output-file))))))))

(defn print-help []
  (println "
Bundle: produce single JS file for usage with bundlers.

Usage:

  nbb bundle <input.cljs> [opts]

Options:

 -o, --out [file]  Write to file instead of stdout.
 --shrinkwrap      Create a single file executable with node deps compiled in."))

(defn init []
  (let [{:keys [bundle-opts]} @opts
        {:keys [cmds args]
         parsed-opts :opts} bundle-opts
        help (:help parsed-opts)
        bundle-file (or (first cmds)
                        (first args))
        js-libs (atom ())
        built-ins (atom [])
        expressions (atom ())
        out (atom "")
        print! #(swap! out
                       (fn [output]
                         (str
                          output
                          (when (not= "" output)
                            "\n")
                          %)))
        ;; Reagent is loaded according to following scheme:
        ;; reagent.core => ./nbb_reagent.js + "react"
        ;; reagent.ratom => ./nbb_reagent.js
        ;; reagent.dom.server => "react" + "react-dom/server" + "./nbb_reagent_dom_server.js"
        initial-ctx (store/get-ctx)
        built-in-nss (sci/eval-string* initial-ctx "(set (map ns-name (all-ns)))")
        ctx (sci/merge-opts initial-ctx
                            {:load-fn (fn [{:keys [namespace ctx]}]
                                        (let [feat (get nbb/feature-requires namespace)]
                                          (cond (string? namespace)
                                                (swap! js-libs conj
                                                       (first (nbb/split-libname namespace)))
                                                (= 'reagent.core namespace)
                                                (do (swap! built-ins conj "./nbb_reagent.js")
                                                    (swap! js-libs conj
                                                           "react"))
                                                (= 'reagent.ratom namespace)
                                                (swap! built-ins conj "./nbb_reagent.js")
                                                (= 'reagent.dom.server namespace)
                                                (do (swap! built-ins conj "./nbb_reagent_dom_server.js")
                                                    (swap! js-libs conj
                                                           "react" "react-dom/server"))
                                                feat
                                                (swap! built-ins conj feat)
                                                (and (symbol? namespace) (not (contains? built-in-nss namespace)))
                                                (let [munged (-> (str namespace)
                                                                 (str/replace
                                                                  "." "/")
                                                                 (str/replace "-" "_"))
                                                      file (nbb/find-file-on-classpath munged)
                                                      file (fs/readFileSync
                                                            file
                                                            "utf-8")]
                                                  (swap! expressions conj file)
                                                  (uberscript {:ctx ctx
                                                               :expressions
                                                               [file]}))))
                                        {})})]
    (if help
      (print-help)
      (binding [nbb/*old-require* true]
        (let [file (fs/readFileSync bundle-file "utf-8")]
          (swap! expressions conj file)
          (uberscript {:ctx ctx
                       :expressions [file]}))
        (print! (gstring/format "import { loadFile, loadString, registerModule } from '%s'"
                                (nbb/npm-lib-name)))
        (doseq [lib (distinct @js-libs)]
          (let [internal (munge lib)
                js-internal (str/replace (str internal) "." "_dot_")]
            (print! (gstring/format "import * as %s from '%s'" js-internal lib))
            (print! (gstring/format "registerModule(%s, '%s')" js-internal lib))))
        (doseq [lib (distinct @built-ins)]
          (print! (gstring/format "import '%s/lib/%s'" (nbb/npm-lib-name) lib)))
        (doseq [expr (distinct @expressions)]
          (print! (gstring/format "await loadString(%s, {disableConfig: true})" (pr-str expr))))
        (let [out-file (:out parsed-opts)
              shrinkwrap? (:shrinkwrap parsed-opts)]
          (cond shrinkwrap?
                (if out-file
                  (build-executable @out out-file)
                  (do (js/console.error "Error: --out option is required when using --shrinkwrap.")
                      (js/Promise.resolve)))

                out-file
                (do (fs/writeFileSync out-file @out "utf-8")
                    (js/Promise.resolve))

                :else
                (do (println @out)
                    (js/Promise.resolve))))))))
