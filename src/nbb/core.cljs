(ns nbb.core
  (:refer-clojure :exclude [load-file time])
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["url" :as url]
   [clojure.string :as str]
   [goog.object :as gobj]
   [goog.string :as gstr]
   [nbb.common :refer [core-ns]]
   [sci.core :as sci]
   [sci.impl.vars :as vars]
   [shadow.esm :as esm])
  (:require-macros [nbb.macros :refer [with-async-bindings]]))

(def opts (atom nil))

(def universe goog/global)

(def cwd (.cwd js/process))

(def command-line-args (sci/new-dynamic-var '*command-line-args* nil {:ns core-ns}))
(def warn-on-infer     (sci/new-dynamic-var '*warn-on-infer* false {:ns core-ns}))

(def ctx (atom {}))

(def nbb-ns (sci/create-ns 'nbb.core nil))

(def sci-ctx (atom nil))

(set! (.-import goog/global) esm/dynamic-import)

(def loaded-modules (atom {}))

(declare load-file)
(declare handle-libspecs)

(def normalize-libname
  {'clojure.pprint 'cljs.pprint})

(defn load-module [m libname as refer rename libspecs]
  (-> (esm/dynamic-import m)
      (.then (fn [_module]
               (let [nlib (normalize-libname libname)]
                 (when-not (= nlib libname)
                   (when as
                     (sci/eval-form @sci-ctx
                                    (list 'alias
                                          (list 'quote nlib)
                                          (list 'quote libname)))))
                 (let [libname (or nlib libname)]
                   (when as
                     (sci/eval-form @sci-ctx
                                    (list 'alias
                                          (list 'quote as)
                                          (list 'quote libname))))
                   (when (seq refer)
                     (sci/eval-form @sci-ctx
                                    (list 'clojure.core/refer
                                          (list 'quote libname)
                                          :only (list 'quote refer)
                                          :rename (list 'quote rename)))))
                 (handle-libspecs (next libspecs)))))))

(def ^:private  windows?
  (= "win32" js/process.platform))

(defn ^:private handle-libspecs [libspecs]
  (if (seq libspecs)
    (let [fst (first libspecs)
          [libname & opts] (if (symbol? fst)
                             [fst] fst)
          opts (apply hash-map opts)
          as (:as opts)
          refer (:refer opts)
          rename (:rename opts)
          munged (munge libname)
          current-ns-str (str @sci/ns)
          current-ns (symbol current-ns-str)]
      (case libname
        ;; built-ins
        (reagent.core reagent.dom reagent.dom.server)
        (let [internal-name (symbol "nbb.internal.react")]
          (let [mod
                ;; NOTE: react could already have been loaded by requiring it
                ;; directly, in that case it's part of loaded-modules already
                (or (get @loaded-modules internal-name)
                    (let [mod ((:require @ctx) "react")]
                      (swap! loaded-modules assoc internal-name mod)
                      mod))]
            ;; To make sure reagent sees the required react, we set it here Wwe
            ;; could make reagent directly use loaded-modules via a global so we
            ;; don't have to hardcode this.
            (set! ^js (.-nbb$internal$react goog/global) mod))
          (load-module "./nbb_reagent.js" libname as refer rename libspecs))
        (promesa.core)
        (load-module "./nbb_promesa.js" libname as refer rename libspecs)
        (applied-science.js-interop)
        (load-module "./nbb_js_interop.js" libname as refer rename libspecs)
        (cljs.pprint clojure.pprint)
        (load-module "./nbb_pprint.js" libname as refer rename libspecs)
        (if (string? libname)
          ;; TODO: parse properties
          (let [[libname properties] (str/split libname #"\$" 2)
                properties (when properties (.split properties "."))
                internal-name (symbol (str "nbb.internal." munged))
                after-load (fn [mod]
                             (swap! loaded-modules assoc internal-name mod)
                             (when as
                               (swap! sci-ctx sci/merge-opts {:classes {internal-name mod}})
                               ;; HACK, we register the alias as a reference to the class
                               ;; via :imports we should expose this functionality in SCI
                               ;; itself as this relies on the internal representation of
                               (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports as] internal-name))
                             (doseq [field refer]
                               (let [mod-field (gobj/get mod (str field))
                                     ;; different namespaces can have different mappings
                                     internal-subname (str internal-name "$" current-ns-str "$" field)]
                                 (swap! sci-ctx sci/merge-opts {:classes {internal-subname mod-field}})
                                 ;; Repeat hack from above
                                 (let [field (get rename field field)]
                                   (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports field] internal-subname))))
                             (handle-libspecs (next libspecs)))
                mod (js/Promise.resolve
                     (or
                      ;; skip loading if module was already loaded
                      (get @loaded-modules internal-name)
                      ;; else load module and register in loaded-modules under internal-name
                      (esm/dynamic-import
                       (let [path ((.-resolve (:require @ctx)) libname)
                             ;; ensure URL on Windows
                             path (if (and windows? (fs/existsSync path))
                                    (str (url/pathToFileURL path))
                                    path)]
                         path))))]
            (-> mod
                (.then (fn [mod]
                         (if properties
                           (gobj/getValueByKeys mod properties)
                           mod)))
                (.then after-load)))
          ;; assume symbol
          (if (sci/eval-form @sci-ctx (list 'clojure.core/find-ns (list 'quote libname)))
            ;; built-in namespace
            (do (sci/eval-form @sci-ctx (list 'require (list 'quote fst)))
                (handle-libspecs (next libspecs)))
            (let [file (str/replace (str munged) #"\." "/")
                  files [(str file ".cljs") (str file ".cljc")]
                  dirs (-> @ctx :classpath :dirs)
                  the-file (reduce (fn [_ dir]
                                     (some (fn [f]
                                             (let [f (path/resolve dir f)]
                                               (when (fs/existsSync f)
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
                                              (list 'quote libname)
                                              :only (list 'quote refer)
                                              :rename (list 'quote rename))))))
                    (.then (fn [_]
                             (handle-libspecs (next libspecs)))))
                ;; here, let's look for classes
                (if-let [clazz (get-in @sci-ctx [:class->opts libname :class])]
                  (do (when as
                        (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports as] libname))
                      (doseq [field refer]
                        (let [mod-field (gobj/get clazz (str field))
                              internal-subname (str current-ns "$" munged "$" field)]
                          (swap! sci-ctx sci/merge-opts {:classes {internal-subname mod-field}})
                          ;; Repeat hack from above
                          (let [field (get rename field field)]
                            (swap! (:env @sci-ctx)
                                   assoc-in
                                   [:namespaces current-ns :imports field] internal-subname))))
                      (handle-libspecs (next libspecs)))
                  (js/Promise.reject (js/Error. (str "Could not find namespace: " libname))))))))))
    (js/Promise.resolve @sci/ns)))

(defn eval-ns-form [ns-form]
  ;; the parsing is still very crude, we only support a subset of the ns form
  ;; and ignore everything but (:require clauses)
  (let [[_ns ns-name & ns-forms] ns-form
        grouped (group-by (fn [ns-form]
                            (and (seq? ns-form)
                                 (= :require (first ns-form)))) ns-forms)
        require-forms (get grouped true)
        other-forms (get grouped false)
        ns-obj (sci/eval-form @sci-ctx (list 'do (list* 'ns ns-name other-forms) '*ns*))
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
  (with-async-bindings {warn-on-infer @warn-on-infer}
    (eval-string* s)))

(defn slurp
  "Asynchronously returns string from file f. Returns promise."
  [f]
  (js/Promise.
   (fn [resolve reject]
     (fs/readFile f
                  (fn [error contents]
                    (if error
                      (reject error)
                      (resolve (str contents))))))))

(defn load-file
  [f]
  (with-async-bindings {sci/file (path/resolve f)}
    (-> (slurp f)
        (.then (fn [source]
                 (load-string source))))))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))

(defn ^:macro time
  "Evaluates expr and prints the time it took. Returns the value of expr."
  [_ _ expr]
  `(let [start# (cljs.core/system-time)
         ret# ~expr]
     (prn (cljs.core/str "Elapsed time: "
                         (.toFixed (- (system-time) start#) 6)
                         " msecs"))
     ret#))

(defn ^:macro implements?* [_ _ psym x]
  ;; hardcoded implementation of implements? for js-interop destructure which
  ;; uses implements?
  (case psym
    cljs.core/ISeq (implements? ISeq x)
    cljs.core/INamed (implements? INamed x)))

(reset! sci-ctx
        (sci/init
         {:namespaces {'clojure.core {'*command-line-args* command-line-args
                                      '*warn-on-infer* warn-on-infer
                                      'time (sci/copy-var time core-ns)
                                      'system-time (sci/copy-var system-time core-ns)
                                      'implements? (sci/copy-var implements?* core-ns)
                                      'array (sci/copy-var array core-ns)}
                       ;; fixes (require 'cljs.core)
                       'cljs.core {}
                       'nbb.core {'load-string (sci/copy-var load-string nbb-ns)
                                  'slurp (sci/copy-var slurp nbb-ns)
                                  'load-file (sci/copy-var load-file nbb-ns)
                                  '*file* sci/file}}
          :classes {'js universe :allow :all
                    'goog.object #js {:get gobj/get
                                      :set gobj/set
                                      :getKeys gobj/getKeys
                                      :getValueByKeys gobj/getValueByKeys}
                    'goog.string #js {:StringBuffer gstr/StringBuffer}}
          :disable-arity-checks true}))

(def ^:dynamic *file* sci/file) ;; make clj-kondo+lsp happy

(defn init []
  (enable-console-print!)
  (sci/alter-var-root sci/print-fn (constantly *print-fn*)))

;;;; Scratch

(comment

  (vars/push-thread-bindings {sci/file "hello"})


  )
