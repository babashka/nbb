(ns nbb.impl.bundler
  "Mostly a copy of babashka uberscript, but generating an .mjs file for Node to execute"
  (:require
   ["fs" :as fs]
   [nbb.core :as nbb :refer [opts]]
   [sci.core :as sci]
   [promesa.core :as p]
   [goog.string :as gstring]
   [goog.string.format]
   [clojure.string :as str]))

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
                  sci/ns @sci/ns]
      (doseq [expr expressions]
        (let [rdr (sci/reader expr)]
          ;;(prn :> (sci/parse-next ctx rdr))
          (loop []
            (let [next-val
                  (try (sci/parse-next ctx rdr)
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


(defn init []
  (let [{:keys [bundle-file]} @opts
        java-libs (atom ())
        built-ins (atom [])
        out (atom "")
        print! #(swap! out
                       (fn [output]
                         (str
                          output
                          (when (not= "" output)
                            "\n")
                          %)))
        ctx (sci/merge-opts @nbb.core/sci-ctx
                            {:load-fn (fn [{:keys [namespace ctx]}]
                                        (cond (string? namespace)
                                              (swap! java-libs conj
                                                     (first (nbb/split-libname namespace)))
                                              (= 'promesa.core namespace)
                                              (swap! built-ins conj "nbb/lib/nbb_promesa.js")
                                              (symbol? namespace)
                                              (uberscript {:ctx ctx
                                                           :expressions
                                                           [(fs/readFileSync
                                                             (str (-> (str namespace)
                                                                      (str/replace
                                                                       ;; TODO: Windows
                                                                        "." "/")
                                                                      (str/replace "-" "_"))
                                                                  ".cljs")
                                                             "utf-8")]}))
                                        {})})]
    (uberscript {:ctx ctx
                 :expressions [(fs/readFileSync bundle-file "utf-8")]})
    (print! "import { loadFile, registerModule } from 'nbb'")
    (doseq [lib @java-libs]
      (let [internal (nbb/libname->internal-name lib)
            js-internal (str/replace (str internal) "." "_dot_")]
        (print! (gstring/format "import * as %s from '%s'" js-internal lib))
        (print! (gstring/format "registerModule(%s, '%s')" js-internal lib))))
    (doseq [lib @built-ins]
      (print! (gstring/format "import '%s'" lib)))
    (println @out)))
