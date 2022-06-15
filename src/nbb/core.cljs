(ns nbb.core
  (:refer-clojure :exclude [load-file time])
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["url" :as url]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [edamame.core]
   [goog.object :as gobj]
   [nbb.classpath :as cp]
   [nbb.common :refer [core-ns]]
   [sci.core :as sci]
   [sci.impl.vars :as vars]
   [sci.lang]
   [shadow.esm :as esm])
  (:require-macros [nbb.macros
                    :as macros
                    :refer [with-async-bindings]]))

(def await-counter 0)

(defn await [p]
  (if (instance? js/Promise p)
    (do (set! await-counter (inc await-counter))
        (set! (.-__nbb_await_promise_result ^js p) true)
        p)
    p))

(defn await? [p]
  (and (instance? js/Promise p)
       (.-__nbb_await_promise_result ^js p)))

(def opts (atom nil))

(def repl-requires
  '[[clojure.repl :refer [apropos source dir doc find-doc]]
    [clojure.pprint :refer [pprint]]])

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
  {'clojure.pprint 'cljs.pprint
   'clojure.test 'cljs.test
   'clojure.math 'cljs.math})

(def sci-find-ns (delay (sci/eval-form @sci-ctx 'find-ns)))

(defn load-module [m libname as refer rename libspecs]
  (-> (if (some? (@sci-find-ns libname))
        (js/Promise.resolve nil)
        (esm/dynamic-import m))
      (.then (fn [_module]
               (let [nlib (normalize-libname libname)]
                 (when (and as nlib
                            (not= nlib libname))
                   (sci/eval-form @sci-ctx
                                  (list 'alias
                                        (list 'quote libname)
                                        (list 'quote nlib))))
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

(defn set-react! [mod]
  (set! ^js (.-nbb$internal$react goog/global) mod))

(defn load-react []
  (let [internal-name (symbol "nbb.internal.react")
        mod
        ;; NOTE: react could already have been loaded by requiring it
        ;; directly, in that case it's part of loaded-modules already
        (or (get @loaded-modules internal-name)
            (let [mod ((:require @ctx) "react")]
              (swap! loaded-modules assoc internal-name mod)
              mod))]
    ;; To make sure reagent sees the required react, we set it here Wwe
    ;; could make reagent directly use loaded-modules via a global so we
    ;; don't have to hardcode this.
    (set-react! mod)))

(declare old-require)

(def feature-requires
  (macros/feature-requires))

(defn ^:private handle-libspecs [libspecs]
  (if (seq libspecs)
    (let [fst (first libspecs)
          [libname & opts] (if (symbol? fst)
                             [fst] fst)
          libname (if (= 'cljs.core libname)
                    'clojure.core libname)
          opts (apply hash-map opts)
          as (:as opts)
          as-alias (:as-alias opts)
          refer (concat (:refer opts) (:refer-macros opts))
          rename (:rename opts)
          munged (munge libname)
          current-ns-str (str @sci/ns)
          current-ns (symbol current-ns-str)]
      (if as-alias
        (do (old-require fst)
            (handle-libspecs (next libspecs)))
        (case libname
          ;; built-ins
          (reagent.core)
          (do
            (load-react)
            (load-module "./nbb_reagent.js" libname as refer rename libspecs))
          (reagent.ratom)
          (load-module "./nbb_reagent.js" libname as refer rename libspecs)
          (reagent.dom.server)
          (do
            (load-react)
            (let [internal-name (symbol "nbb.internal.react-dom-server")]
              (let [mod
                    ;; NOTE: react could already have been loaded by requiring it
                    ;; directly, in that case it's part of loaded-modules already
                    (or (get @loaded-modules internal-name)
                        (let [mod ((:require @ctx) "react-dom/server")]
                          (swap! loaded-modules assoc internal-name mod)
                          mod))]
                ;; To make sure reagent sees the required react, we set it here Wwe
                ;; could make reagent directly use loaded-modules via a global so we
                ;; don't have to hardcode this.
                (set! ^js (.-nbb$internal$react-dom-server goog/global) mod))
              (load-module "./nbb_reagent_dom_server.js" libname as refer rename libspecs)))
          (promesa.core)
          (load-module "./nbb_promesa.js" libname as refer rename libspecs)
          (applied-science.js-interop)
          (load-module "./nbb_js_interop.js" libname as refer rename libspecs)
          (cljs-bean.core)
          (load-module "./nbb_cljs_bean.js" libname as refer rename libspecs)
          (cljs.pprint clojure.pprint)
          (load-module "./nbb_pprint.js" libname as refer rename libspecs)
          (cljs.test clojure.test)
          (load-module "./nbb_test.js" libname as refer rename libspecs)
          (nbb.repl)
          (load-module "./nbb_repl.js" libname as refer rename libspecs)
          (clojure.tools.cli)
          (load-module "./nbb_tools_cli.js" libname as refer rename libspecs)
          (goog.string goog.string.format)
          (load-module "./nbb_goog_string.js" libname as refer rename libspecs)
          (goog.crypt)
          (load-module "./nbb_goog_crypt.js" libname as refer rename libspecs)
          (cognitect.transit)
          (load-module "./nbb_transit.js" libname as refer rename libspecs)
          (clojure.data)
          (load-module "./nbb_data.js" libname as refer rename libspecs)
          (cljs.math clojure.math)
          (load-module "./nbb_math.js" libname as refer rename libspecs)
          (schema.core)
          (load-module ((.-resolve (:require @ctx)) "@babashka/nbb-prismatic-schema/index.mjs")
                       libname as refer rename libspecs)
          (malli.core)
          (load-module ((.-resolve (:require @ctx)) "@babashka/nbb-metosin-malli/index.mjs")
                       libname as refer rename libspecs)
          (let [feat (get feature-requires libname)]
            (cond
              feat (load-module feat libname as refer rename libspecs)
              (string? libname)
              ;; TODO: parse properties
              (let [[libname properties] (str/split libname #"\$" 2)
                    properties (when properties (.split properties "."))
                    internal-name (symbol (str "nbb.internal." munged))
                    after-load
                    (fn [mod]
                      (swap! loaded-modules assoc internal-name mod)
                      (when as
                        (swap! sci-ctx
                               (fn [sci-ctx]
                                 (-> sci-ctx
                                     (sci/add-class! internal-name mod)
                                     (sci/add-import! current-ns internal-name as)))))
                      (doseq [field refer]
                        (let [mod-field (gobj/get mod (str field))
                              ;; different namespaces can have different mappings
                              internal-subname (str internal-name "$" current-ns-str "$" field)
                              field (get rename field field)]
                          (swap! sci-ctx
                                 (fn [sci-ctx]
                                   (-> sci-ctx
                                       (sci/add-class! internal-subname mod-field)
                                       (sci/add-import! current-ns internal-subname field))))))
                      (handle-libspecs (next libspecs)))
                    mod (js/Promise.resolve
                         (or
                          ;; skip loading if module was already loaded
                          (get @loaded-modules internal-name)
                          ;; else load module and register in loaded-modules under internal-name
                          (->
                           (js/Promise.resolve
                            (if (str/starts-with? libname "./")
                              (path/resolve (path/dirname @sci/file) libname)
                              (try ((.-resolve (:require @ctx)) libname)
                                   (catch :default _
                                     ((:resolve @ctx) libname)))))
                           (.then (fn [path]
                                    (esm/dynamic-import
                                     (let [path (if (and windows? (fs/existsSync path))
                                                  (str (url/pathToFileURL path))
                                                  path)]
                                       path))))
                           (.then (fn [mod]
                                    (if properties
                                      (gobj/getValueByKeys mod properties)
                                      mod))))))]
                (-> mod
                    (.then after-load)))
              :else
              ;; assume symbol
              (if (sci/eval-form @sci-ctx (list 'clojure.core/find-ns (list 'quote libname)))
                ;; built-in namespace
                (do (old-require fst)
                    (handle-libspecs (next libspecs)))
                (let [file (str/replace (str munged) #"\." "/")
                      files [(str file ".cljs") (str file ".cljc")]
                      dirs @cp/classpath-entries
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
                    (js/Promise.reject (js/Error. (str "Could not find namespace: " libname)))))))))))
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
        ;; ignore all :require-macros for now
        other-forms (remove #(and (seq? %) (= :require-macros (first %)))
                            other-forms)
        ns-obj (sci/eval-form @sci-ctx (list 'do (list* 'ns ns-name other-forms) '*ns*))
        libspecs (mapcat rest
                         require-forms)]
    (with-async-bindings {sci/ns ns-obj}
      (handle-libspecs libspecs))))

(defn eval-require [require-form]
  (let [args (rest require-form)
        libspecs (mapv #(sci/eval-form @sci-ctx %) args)]
    (handle-libspecs libspecs)))

(defn parse-next [reader]
  (sci/parse-next @sci-ctx reader
                  {:features #{:org.babashka/nbb
                               :cljs}}))

(declare eval-next)

(defn eval-seq [reader form opts]
  (let [fst (first form)]
    (cond (= 'do fst)
          (reduce (fn [acc form]
                    (.then acc (fn [_]
                                 (eval-seq reader form opts))))
                  (js/Promise.resolve nil)
                  (rest form))
          (= 'ns fst)
          (.then (eval-ns-form form)
                 (fn [ns-obj]
                   (eval-next ns-obj reader opts)))
          :else
          (try (let [pre-await await-counter
                     next-val (sci/eval-form @sci-ctx form)
                     post-await await-counter]
                 (if (= pre-await post-await)
                   (.then (js/Promise.resolve nil)
                          (fn [_]
                            (eval-next next-val reader opts)))
                   (cond
                     (instance? sci.lang/Var next-val)
                     (let [v (deref next-val)]
                       (if (await? v)
                         (.then v
                                (fn [v]
                                  (sci/alter-var-root next-val (constantly v))
                                  (eval-next next-val reader opts)))
                         (eval-next next-val reader opts)))
                     (await? next-val)
                     (.then next-val
                            (fn [v]
                              (eval-next v reader opts)))
                     :else (.then (js/Promise.resolve nil)
                                  (fn [_]
                                    (eval-next next-val reader opts))))))
               (catch :default e
                 (js/Promise.reject e))))))

(deftype Reject [v])

(defn eval-next
  "Evaluates top level forms asynchronously. Returns promise of last value."
  ([prev-val reader] (eval-next prev-val reader nil))
  ([prev-val reader opts]
   (let [next-val (try (if-let [parse-fn (:parse-fn opts)]
                         (parse-fn reader)
                         (parse-next reader))
                       (catch :default e
                         (js/Promise.reject e)))]
     (if (instance? js/Promise next-val)
       next-val
       (if (not= :sci.core/eof next-val)
         (if (seq? next-val)
           (eval-seq reader next-val opts)
           (let [v (try (sci/eval-form @sci-ctx next-val)
                        (catch :default e
                          (->Reject e)))]
             (if (instance? Reject v)
               (js/Promise.reject (.-v v))
               (recur v reader opts))))
         ;; wrap normal value in promise
         (js/Promise.resolve
          (let [wrap (or (:wrap opts)
                         identity)]
            (wrap prev-val))))))))

(defn eval-string* [s]
  (with-async-bindings {sci/ns @sci/ns}
    (let [reader (sci/reader s)]
      (eval-next nil reader))))

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
        (.then load-string))))

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

(defn ^:macro time*
  "Async version of time."
  [_ _ expr]
  `(let [start# (cljs.core/system-time)
         ret# ~expr
         ret# (js/Promise.resolve ret#)]
     (nbb.core/await
      (.then ret# (fn [v#]
                    (prn (cljs.core/str "Elapsed time: "
                                        (.toFixed (- (cljs.core/system-time) start#) 6)
                                        " msecs"))
                    v#)))))

(defn ^:macro implements?* [_ _ psym x]
  ;; hardcoded implementation of implements? for js-interop destructure which
  ;; uses implements?
  (case psym
    cljs.core/ISeq (implements? ISeq x)
    cljs.core/INamed (implements? INamed x)))

(def cp-ns (sci/create-ns 'nbb.classpath nil))

(defn version []
  (macros/get-in-package-json :version))

(defn cli-name []
  (macros/cli-name))

(reset! sci-ctx
        (sci/init
         {:namespaces {'clojure.core {'*command-line-args* command-line-args
                                      '*warn-on-infer* warn-on-infer
                                      'time (sci/copy-var time core-ns)
                                      'system-time (sci/copy-var system-time core-ns)
                                      'implements? (sci/copy-var implements?* core-ns)
                                      'array (sci/copy-var array core-ns)
                                      'tap> (sci/copy-var tap> core-ns)
                                      'add-tap (sci/copy-var add-tap core-ns)
                                      'remove-tap (sci/copy-var remove-tap core-ns)
                                      'uuid (sci/copy-var uuid core-ns)
                                      'IEditableCollection (sci/copy-var IEditableCollection core-ns)
                                      'MapEntry (sci/copy-var MapEntry core-ns)
                                      'UUID (sci/copy-var UUID core-ns)
                                      'PersistentQueue (sci/copy-var PersistentQueue core-ns)
                                      'update-vals (sci/copy-var update-vals core-ns)
                                      'update-keys (sci/copy-var update-keys core-ns)
                                      'iteration (sci/copy-var iteration core-ns)
                                      'NaN? (sci/copy-var NaN? core-ns)
                                      'parse-long (sci/copy-var parse-long core-ns)
                                      'parse-double (sci/copy-var parse-double core-ns)
                                      'parse-boolean (sci/copy-var parse-boolean core-ns)
                                      'parse-uuid (sci/copy-var parse-uuid core-ns)}
                       'cljs.reader {'read-string (sci/copy-var edn/read-string (sci/create-ns 'cljs.reader))}
                       'clojure.main {'repl-requires (sci/copy-var
                                                      repl-requires
                                                      (sci/create-ns 'clojure.main))}

                       'nbb.core {'load-string (sci/copy-var load-string nbb-ns)
                                  'load-file (sci/copy-var load-file nbb-ns)
                                  'alter-var-root (sci/copy-var sci/alter-var-root nbb-ns)
                                  'slurp (sci/copy-var slurp nbb-ns)
                                  '*file* sci/file
                                  'version (sci/copy-var version nbb-ns)
                                  'await (sci/copy-var await nbb-ns)
                                  'time (sci/copy-var time* nbb-ns)}
                       'nbb.classpath {'add-classpath (sci/copy-var cp/add-classpath cp-ns)
                                       'get-classpath (sci/copy-var cp/get-classpath cp-ns)}
                       'goog.object {'get gobj/get
                                     'set gobj/set
                                     'getKeys gobj/getKeys
                                     'getValueByKeys gobj/getValueByKeys}
                       'edamame.core (sci/copy-ns edamame.core (sci/create-ns 'edamame.core))}
          :classes {'js universe :allow :all
                    'goog.object #js {:get gobj/get
                                      :set gobj/set
                                      :getKeys gobj/getKeys
                                      :getValueByKeys gobj/getValueByKeys}
                    'ExceptionInfo ExceptionInfo
                    'Math js/Math}
          :disable-arity-checks true}))

(def old-require (sci/eval-form @sci-ctx 'require))

(swap! (:env @sci-ctx) assoc-in
       [:namespaces 'clojure.core 'require]
       (fn [& args] (await (.then (handle-libspecs args)
                                  (fn [_])))))

(def ^:dynamic *file* sci/file) ;; make clj-kondo+lsp happy

(defn init []
  (enable-console-print!)
  (sci/alter-var-root sci/print-fn (constantly *print-fn*))
  (sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*)))

;;;; Scratch

(comment

  (vars/push-thread-bindings {sci/file "hello"})


  )
