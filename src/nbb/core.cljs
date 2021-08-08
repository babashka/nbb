(ns nbb.core
  (:refer-clojure :exclude [load-file])
  (:require
   ["path" :as node:path]
   ["fs" :as node:fs]
   [clojure.string :as str]
   [goog.object :as gobj]
   [sci.core :as sci]
   [sci.impl.vars :as vars]
   [shadow.esm :as esm])
  (:require-macros [nbb.macros :refer [with-async-bindings]]))

(def universe goog/global)

(def cwd (.cwd js/process))

(def core-ns (sci/create-ns 'clojure.core nil))

(def command-line-args (sci/new-dynamic-var '*command-line-args* nil {:ns core-ns}))

(def ctx (atom {}))

;; (declare eval-expr load-string slurp load-file)

(def nbb-ns (sci/create-ns 'nbb.core nil))

(def sci-ctx (atom nil))

(set! (.-import goog/global) esm/dynamic-import)

(def loaded-modules (atom {}))

(declare load-file)

;; workaround for bug in shadow-cljs when requiring node modules from different
;; namespaces under advanced compilation
(def path node:path)
(def path:resolve (.-resolve node:path))
(def path:delimiter (.-delimiter node:path))
(def path:is-absolute (.-isAbsolute node:path))

(def fs node:path)
(def fs:exists (.-existsSync node:fs))
(def fs:read-file-sync (.-readFileSync node:fs))

(defn handle-libspecs [libspecs]
  (if (seq libspecs)
    (let [fst (first libspecs)
          [libname & opts] fst
          opts (apply hash-map opts)
          as (:as opts)
          refer (:refer opts)]
      (case libname
        ;; built-ins
        (reagent.core reagent.dom reagent.dom.server)
        (-> (esm/dynamic-import "./nbb_reagent.js")
            (.then (fn [_reagent]
                     (when as
                       (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname))))
                     (handle-libspecs (next libspecs)))))
        ;; default
        (if (string? libname)
          ;; TODO: parse properties
          (let [[libname _properties] (str/split libname #"\\$")
                internal-name (symbol (str "nbb.internal." (munge libname)))
                mod (or
                     ;; skip loading if module was already loaded
                     (get @loaded-modules internal-name)
                     ;; else load module and register in loaded-modules under internal-name
                     (let [mod ((:require @ctx) libname)]
                       (swap! loaded-modules assoc internal-name mod)
                       mod))
                current-ns (symbol (str @sci/ns))]
            (when as
              (swap! sci-ctx sci/merge-opts {:classes {internal-name mod}})
              ;; HACK, we register the alias as a reference to the class
              ;; via :imports we should expose this functionality in SCI
              ;; itself as this relies on the internal representation of
              (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports as] internal-name))
            (doseq [field refer]
              (let [mod-field (gobj/get mod (str field))
                    internal-subname (str internal-name "$" field)]
                (swap! sci-ctx sci/merge-opts {:classes {internal-subname mod-field}})
                ;; Repeat hack from above
                (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports field] internal-subname)))
            (handle-libspecs (next libspecs)))
          ;; assume symbol
          (if (sci/eval-form @sci-ctx (list 'clojure.core/find-ns (list 'quote libname)))
            ;; built-in namespace
            (do (sci/eval-form @sci-ctx (list 'require (list 'quote fst)))
                (js/Promise.resolve @sci/ns))
            (let [munged (munge libname)
                  file (str/replace (str munged) #"\." "/")
                  files [(str file ".cljs") (str file ".cljc")]
                  dirs (-> @ctx :classpath :dirs)
                  the-file (reduce (fn [_ dir]
                                     (some (fn [f]
                                             (let [f (path:resolve dir f)]
                                               (when (fs:exists f)
                                                 (reduced f))))
                                           files)) nil dirs)]
              (if the-file
                (-> (load-file the-file)
                    (.then
                     (fn [_]
                       (when as
                         (sci/eval-form @sci-ctx
                                        (list 'clojure.core/alias
                                              (list 'quote as)
                                              (list 'quote libname))))
                       (when (seq refer)
                         (sci/eval-form @sci-ctx
                                        (list 'clojure.core/refer
                                              (list 'quote libname) :only (list 'quote refer))))))
                    (.then (fn [_]
                             (handle-libspecs (next libspecs)))))
                (js/Promise.reject (js/Error. (str "Could not find namespace: " libname)))))))))
    (js/Promise.resolve @sci/ns)))

(defn eval-ns-form [ns-form]
  ;; the parsing is still very crude, we only support a subset of the ns form
  ;; and ignore everything but (:require clauses)
  (let [[_ns ns-name & ns-forms] ns-form
        ns-obj (sci/eval-form @sci-ctx (list 'do (list 'ns ns-name) '*ns*))
        require-forms (filter (fn [ns-form]
                                (and (seq? ns-form)
                                     (= :require (first ns-form))))
                              ns-forms)
        libspecs (mapcat (fn [require-form]
                           (rest require-form))
                         require-forms)]
    (with-async-bindings {sci/ns ns-obj}
      (handle-libspecs libspecs))))

(defn eval-require [require-form]
  (let [args (rest require-form)
        libspecs (mapv #(sci/eval-form @sci-ctx %) args)]
    (handle-libspecs libspecs)))

(defn eval-expr
  "Evaluates top level forms asynchronously. Returns promise of last value."
  [prev-val reader]
  (let [next-val (try
                   (sci/parse-next @sci-ctx reader {:features #{:cljs}})
                   (catch :default e
                     (js/Promise.reject e)))]
    (if-not (= :sci.core/eof next-val)
      (if (seq? next-val)
        (let [fst (first next-val)]
          (cond (= 'ns fst)
                ;; async
                (.then (eval-ns-form next-val)
                       (fn [ns-obj]
                         (eval-expr ns-obj reader)))
                (= 'require fst)
                ;; async
                (.then (eval-require next-val)
                       (fn [_]
                         (eval-expr nil reader)))
                :else
                (try
                  (eval-expr (sci/eval-form @sci-ctx next-val) reader)
                     (catch :default e
                       (js/Promise.reject e)))))
        (try
          (eval-expr (sci/eval-form @sci-ctx next-val) reader)
             (catch :default e
               (js/Promise.reject e))))
      ;; wrap normal value in promise
      (js/Promise.resolve prev-val))))

(defn eval-string* [s]
  (with-async-bindings {sci/ns @sci/ns}
    (let [reader (sci/reader s)]
      (eval-expr nil reader))))

(defn load-string
  "Asynchronously parses and evaluates string s. Returns promise."
  [s]
  (eval-string* s))

(defn slurp
  "Synchronously returns string from file f."
  [f]
  (str (fs:read-file-sync f)))

(defn load-file
  [f]
  (let [source (slurp f)]
    (with-async-bindings {sci/file (path:resolve f)}
      (load-string source))))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))

(reset! sci-ctx
        (sci/init
         {:namespaces {'clojure.core {'prn prn
                                      'print print
                                      'println println
                                      '*command-line-args* command-line-args}
                       'nbb.core {'load-string (sci/copy-var load-string nbb-ns)
                                  'slurp (sci/copy-var slurp nbb-ns)
                                  'load-file (sci/copy-var load-file nbb-ns)
                                  '*file* sci/file}}
          :classes {'js universe :allow :all}
          :disable-arity-checks true}))

(def ^:dynamic *file* sci/file) ;; make clj-kondo+lsp happy

(defn init [])

;;;; Scratch

(comment

  (vars/push-thread-bindings {sci/file "hello"})


  )
