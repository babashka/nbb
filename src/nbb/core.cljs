(ns nbb.core
  (:refer-clojure :exclude [load-file time])
  (:require
   ["fs" :as fs]
   ["path" :as path]
   ["url" :as url]
   [babashka.cli]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [edamame.core]
   [goog.object :as gobj]
   [nbb.classpath :as cp]
   [nbb.common :refer [core-ns]]
   [sci.core :as sci]
   [sci.ctx-store :as store]
   [sci.impl.unrestrict :refer [*unrestricted*]]
   [sci.impl.vars :as vars]
   [sci.lang]
   [shadow.esm :as esm]
   [cljs.tools.reader.reader-types]
   [nbb.error :as nbb.error])
  (:require-macros [nbb.macros :as macros]))

(set! *unrestricted* true)

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

(set! (.-import goog/global)
      (fn [what]
        ;; need to resolve based on the current file
        (-> ((:resolve @ctx) what)
            (.then #(esm/dynamic-import %)))))

(def loaded-modules (atom {}))

(declare load-file)
(declare handle-libspecs)

(def normalize-libname
  {'clojure.pprint 'cljs.pprint
   'clojure.test 'cljs.test
   'clojure.math 'cljs.math})

(def sci-find-ns (delay (sci/eval-form (store/get-ctx) 'find-ns)))

(defn load-module [m libname as refer rename libspecs opts]
  (-> (if (some? (@sci-find-ns libname))
        (js/Promise.resolve nil)
        (esm/dynamic-import m))
      (.then (fn [_module]
               (let [nlib (normalize-libname libname)]
                 (sci/binding [sci/ns (:ns opts)]
                   (when (and as nlib
                              (not= nlib libname))
                     (sci/eval-form (store/get-ctx)
                                    (list 'alias
                                          (list 'quote libname)
                                          (list 'quote nlib))))
                   (let [libname (or nlib libname)]
                     (when as
                       (sci/eval-form (store/get-ctx)
                                      (list 'alias
                                            (list 'quote as)
                                            (list 'quote libname))))
                     (when (seq refer)
                       (sci/eval-form (store/get-ctx)
                                      (list 'clojure.core/refer
                                            (list 'quote libname)
                                            :only (list 'quote refer)
                                            :rename (list 'quote rename))))))
                 (handle-libspecs (next libspecs) opts))))))

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

(defn npm-lib-name []
  (macros/npm-lib-name))

(defn split-libname [libname]
  (str/split libname #"\$" 2))

(defn register-module [mod internal-name]
  (swap! loaded-modules assoc internal-name mod))

(defn load-js-module [libname internal-name]
  (-> (if-let [resolve (:resolve @ctx)]
        (-> (resolve libname)
            (.catch
             (fn [_]
               ((.-resolve (:require @ctx)) libname))))
        (js/Promise.resolve ((.-resolve (:require @ctx)) libname)))
      (.then (fn [path]
               (esm/dynamic-import
                (let [path (if (and windows? (fs/existsSync path))
                             (str (url/pathToFileURL path))
                             path)]
                  path))))
      (.then (fn [mod]
               (register-module mod internal-name)
               mod))))

(defn munged->internal [munged]
  (symbol (str "nbb.internal." munged)))

(defn libname->internal-name [libname]
  (-> libname munge munged->internal))

(defn find-file-on-classpath [munged]
  (let [file (str/replace (str munged) #"\." "/")
        files [(str file ".cljs") (str file ".cljc")]
        dirs @cp/classpath-entries]
    (reduce (fn [_ dir]
              (some (fn [f]
                      (let [f (path/resolve dir f)]
                        (when (fs/existsSync f)
                          (reduced f))))
                    files)) nil dirs)))

;; Reagent is loaded according to following scheme:
;; reagent.core => ./nbb_reagent.js + "react"
;; reagent.ratom => ./nbb_reagent.js
;; reagent.dom.server => "react" + "react-dom/server" + "./nbb_reagent_dom_server.js"

(defn ^:private handle-libspecs [libspecs ns-opts]
  ;; (assert (:ns ns-opts) "ns")
  ;; (assert (:file ns-opts) "file")
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
          current-ns-str (str (:ns ns-opts))
          current-ns (symbol current-ns-str)]
      (if
          ;; this handles the :require-macros self-require case
          (= libname current-ns)
        (handle-libspecs (next libspecs) ns-opts)
        (if as-alias
          (do (old-require fst)
              (handle-libspecs (next libspecs) ns-opts))
          (case libname
            ;; built-ins
            (reagent.core)
            (do
              (load-react)
              (load-module "./nbb_reagent.js" libname as refer rename libspecs ns-opts))
            (reagent.ratom)
            (load-module "./nbb_reagent.js" libname as refer rename libspecs ns-opts)
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
                (load-module "./nbb_reagent_dom_server.js" libname as refer rename libspecs ns-opts)))
            ;; (schema.core)
            ;; (load-module ((.-resolve (:require @ctx)) "@babashka/nbb-prismatic-schema/index.mjs")
            ;;              libname as refer rename libspecs)
            ;; (malli.core)
            ;; (load-module ((.-resolve (:require @ctx)) "@babashka/nbb-metosin-malli/index.mjs")
            ;;              libname as refer rename libspecs)
            (let [feat (get feature-requires libname)]
              (cond
                feat (load-module feat libname as refer rename libspecs ns-opts)
                (string? libname)
                ;; TODO: parse properties
                (let [libname (if (str/starts-with? libname "./")
                                (path/resolve (path/dirname (:file ns-opts)) libname)
                                libname)
                      [libname properties*] (split-libname libname)
                      munged (munge libname)
                      properties (when properties* (.split properties* "."))
                      internal-name (munged->internal munged)
                      after-load
                      (fn [mod]
                        (let [internal-name
                              (if properties*
                                (str internal-name "$" properties*)
                                internal-name)]
                          (when as
                            (store/swap-ctx!
                             (fn [sci-ctx]
                               (-> sci-ctx
                                   (sci/add-class! internal-name mod)
                                   (sci/add-import! current-ns internal-name as))))))
                        (doseq [field refer]
                          (let [mod-field (gobj/get mod (str field))
                                ;; different namespaces can have different mappings
                                internal-subname (str internal-name "$" current-ns-str "$" field)
                                field (get rename field field)]
                            (store/swap-ctx!
                             (fn [sci-ctx]
                               (-> sci-ctx
                                   (sci/add-class! internal-subname mod-field)
                                   (sci/add-import! current-ns internal-subname field))))))
                        (handle-libspecs (next libspecs) ns-opts))
                      mod (js/Promise.resolve
                           (->
                            (or
                             ;; skip loading if module was already loaded
                             (some-> (get @loaded-modules internal-name)
                                     js/Promise.resolve)
                             (load-js-module libname internal-name)
                             ;; else load module and register in loaded-modules under internal-name
                             )
                            (.then (fn [mod]
                                     (if properties
                                       (gobj/getValueByKeys mod properties)
                                       mod)))))]
                  (-> mod
                      (.then after-load)))
                :else
                ;; assume symbol
                (if (sci/eval-form (store/get-ctx) (list 'clojure.core/find-ns (list 'quote libname)))
                  ;; built-in namespace
                  (do (sci/binding [sci/ns (:ns ns-opts)
                                    sci/file (:file ns-opts)]
                        (old-require fst))
                      (handle-libspecs (next libspecs) ns-opts))
                  (if-let [the-file (find-file-on-classpath munged)]
                    (-> (load-file the-file)
                        (.then
                         (fn [_]
                           (sci/binding [sci/ns (:ns ns-opts)]
                             (when as
                               (sci/eval-form (store/get-ctx)
                                              (list 'clojure.core/alias
                                                    (list 'quote as)
                                                    (list 'quote libname))))
                             (when (seq refer)
                               (sci/eval-form (store/get-ctx)
                                              (list 'clojure.core/refer
                                                    (list 'quote libname)
                                                    :only (list 'quote refer)
                                                    :rename (list 'quote rename)))))))
                        (.then (fn [_]
                                 (handle-libspecs (next libspecs) ns-opts))))
                    (js/Promise.reject (js/Error. (str "Could not find namespace: " libname)))))))))))
    (js/Promise.resolve (:ns ns-opts))))

(defn eval-ns-form [ns-form opts]
  ;; the parsing is still very crude, we only support a subset of the ns form
  ;; and ignore everything but (:require clauses)
  (let [[_ns ns-name & ns-forms] ns-form
        grouped (group-by (fn [ns-form]
                            (and (seq? ns-form)
                                 (let [fst (first ns-form)]
                                   (or (= :require fst)
                                       (= :require-macros fst))))) ns-forms)
        require-forms (get grouped true)
        other-forms (get grouped false)
        ;; ignore all :require-macros for now
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form (store/get-ctx) (list 'do (list* 'ns ns-name other-forms) '*ns*)))
        libspecs (mapcat rest require-forms)
        opts (assoc opts :ns ns-obj)]
    (handle-libspecs libspecs opts)))

(defn eval-require [require-form]
  (let [args (rest require-form)
        libspecs (mapv #(sci/eval-form (store/get-ctx) %) args)
        sci-ns @sci/ns
        sci-file @sci/file]
    (handle-libspecs libspecs {:ns sci-ns
                               :file sci-file})))

(defn parse-next [reader]
  (sci/parse-next (store/get-ctx) reader
                  {:features #{:org.babashka/nbb
                               :cljs}}))

(declare eval-next)

(defn eval-simple [form opts]
  (sci/binding [sci/ns (:ns opts)
                sci/file (:file opts)]
    (sci/eval-form (store/get-ctx) form)))

(defn eval-seq [reader form opts eval-next]
  (let [fst (first form)]
    (cond (= 'do fst)
          (reduce (fn [acc form]
                    (.then acc (fn [_]
                                 (if (seq? form)
                                   (eval-seq reader form opts eval-next)
                                   (eval-simple form opts)))))
                  (js/Promise.resolve nil)
                  (rest form))
          (= 'ns fst)
          (.then (eval-ns-form form opts)
                 (fn [ns-obj]
                   (eval-next ns-obj reader (assoc opts :ns ns-obj))))
          :else
          (try (let [pre-await await-counter
                     next-val (sci/binding [sci/ns (:ns opts)
                                            sci/file (:file opts)]
                                (sci/eval-form (store/get-ctx) form))
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

(defn read-next [reader opts]
  (try (sci/binding [sci/ns (:ns opts)]
         (if-let [parse-fn (:parse-fn opts)]
           (parse-fn reader)
           (parse-next reader)))
       (catch :default e
         (js/Promise.reject e))))

(defn eval-next
  "Evaluates top level forms asynchronously. Returns promise of last value."
  [prev-val reader opts]
  (let [next-val (read-next reader opts)]
    (if (instance? js/Promise next-val)
      next-val
      (if (not= :sci.core/eof next-val)
        (if (seq? next-val)
          (eval-seq reader next-val opts eval-next)
          (let [v (eval-simple next-val opts)]
            (recur v reader opts)))
        ;; wrap normal value in promise
        (js/Promise.resolve
         prev-val)))))

(defn reader? [rdr]
  (instance? cljs.tools.reader.reader-types/IndexingPushbackReader rdr))

(def init-sentinel (js/Object.))

(defn -eval-next*
  "Evaluates top level forms asynchronously. Has options for REPL."
  [prev-val next-val opts]
  (if (instance? js/Promise next-val)
    next-val
    (if (identical? init-sentinel prev-val)
      (if (seq? next-val)
        (eval-seq next-val next-val opts -eval-next*)
        (let [v (eval-simple next-val opts)]
          (recur v next-val opts)))
      ;; wrap normal value in promise
      (js/Promise.resolve
       (let [wrap (or (:wrap opts)
                      identity)]
         (wrap prev-val {:ns (:ns opts)}))))))

(defn eval-next*
  "Evaluates top level forms asynchronously. Has options for REPL."
  [val opts]
  (-eval-next* init-sentinel val opts))

(defn eval-string* [s opts]
  (let [reader (sci/reader s)]
    (eval-next nil reader opts)))

(defn load-string
  "Asynchronously parses and evaluates string s. Returns promise."
  [s]
  (let [sci-file @sci/file
        sci-ns @sci/ns]
    (eval-string* s {:ns sci-ns :file sci-file})))

(defn slurp
  "Asynchronously returns string from file f. Returns promise."
  [f]
  (js/Promise.
   (fn [resolve reject]
     (fs/readFile f
                  "utf-8"
                  (fn [error contents]
                    (if error
                      (reject error)
                      (resolve (str contents))))))))

(defn load-file
  [f]
  (let [sci-file (path/resolve f)
        sci-ns @sci/ns]
    (-> (slurp f)
        (.then #(sci/binding [sci/file sci-file
                              sci/ns sci-ns]
                  (load-string %)))
        (await)
        #_(.finally (fn []
                      (prn :finally (str @sci/ns)))))))

(defn register-plugin! [_plug-in-name sci-opts]
  (store/swap-ctx! sci/merge-opts sci-opts))

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
    cljs.core/INamed (implements? INamed x)
    (list 'cljs.core/instance? psym x)))

(def cp-ns (sci/create-ns 'nbb.classpath nil))

(defn version []
  (macros/get-in-package-json :version))

(defn cli-name []
  (macros/cli-name))

(defn ^:macro gdefine [_ _ name value]
  (list 'def name value))

(def ^:private goog-object-ns
  {'add gobj/add
   'clear gobj/clear
   'clone gobj/clone
   'contains gobj/contains
   'containsKey gobj/containsKey
   'containsValue gobj/containsValue
   'create gobj/create
   'createImmutableView gobj/createImmutableView
   'createSet gobj/createSet
   'equals gobj/equals
   'every gobj/every
   'extend gobj/extend
   'filter gobj/filter
   'findKey gobj/findKey
   'findValue gobj/findValue
   'forEach gobj/forEach
   'get gobj/get
   'getAllPropertyNames gobj/getAllPropertyNames
   'getAnyKey gobj/getAnyKey
   'getAnyValue gobj/getAnyValue
   'getCount gobj/getCount
   'getKeys gobj/getKeys
   'getSuperClass gobj/getSuperClass
   'getValueByKeys gobj/getValueByKeys
   'getValues gobj/getValues
   'isEmpty gobj/isEmpty
   'isImmutableView gobj/isImmutableView
   'map gobj/map
   'remove gobj/remove
   'set gobj/set
   'setIfUndefined gobj/setIfUndefined
   'setWithReturnValueIfNotSet gobj/setWithReturnValueIfNotSet
   'some gobj/some
   'transpose gobj/transpose
   'unsafeClone gobj/unsafeClone})

(def cli-ns (sci/create-ns 'babashka.cli nil))

(def cli-namespace
  (sci/copy-ns babashka.cli cli-ns))

(def ens
  (sci/create-ns 'nbb.error))

(defn print-error-report
  ([e]
   (print-error-report e @opts))
  ([e opts]
   (nbb.error/print-error-report e opts)))

(def sns
  (sci/create-ns 'sci.core nil))

(store/reset-ctx!
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
                               'update-vals (sci/copy-var update-vals core-ns)
                               'update-keys (sci/copy-var update-keys core-ns)
                               'iteration (sci/copy-var iteration core-ns)
                               'NaN? (sci/copy-var NaN? core-ns)
                               'parse-long (sci/copy-var parse-long core-ns)
                               'parse-double (sci/copy-var parse-double core-ns)
                               'parse-boolean (sci/copy-var parse-boolean core-ns)
                               'parse-uuid (sci/copy-var parse-uuid core-ns)
                               'goog-define (sci/copy-var gdefine core-ns)
                               'type->str (sci/copy-var type->str core-ns)
                               'Keyword (sci/copy-var Keyword core-ns)
                               'Symbol (sci/copy-var Symbol core-ns)
                               'PersistentVector (sci/copy-var PersistentVector core-ns)
                               'IFn (sci/copy-var IFn core-ns)
                               'swap-vals! (sci/copy-var swap-vals! core-ns)
                               'reset-vals! (sci/copy-var reset-vals! core-ns)}
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
                'nbb.error {'print-error-report (sci/copy-var print-error-report ens)}
                'goog.object goog-object-ns
                'edamame.core (sci/copy-ns edamame.core (sci/create-ns 'edamame.core))
                'babashka.cli cli-namespace
                'sci.core {'stacktrace (sci/copy-var sci/stacktrace sns)
                           'format-stacktrace (sci/copy-var sci/format-stacktrace sns)}}
   :classes {'js universe :allow :all
             'goog.object (clj->js goog-object-ns)
             'ExceptionInfo ExceptionInfo
             'Math js/Math}}))

(sci/enable-unrestricted-access!)

(def old-require (sci/eval-form (store/get-ctx) 'require))

(def ^:dynamic *old-require* false)

(swap! (:env (store/get-ctx)) assoc-in
       [:namespaces 'clojure.core 'require]
       (fn [& args]
         (if *old-require*
           (apply old-require args)
           (await (.then (identity ;; with-async-bindings {sci/file @sci/file}
                          (handle-libspecs args {:ns @sci/ns
                                                 :file @sci/file}))
                         (fn [_]))))))

(def ^:dynamic *file* sci/file) ;; make clj-kondo+lsp happy

(defn init []
  (enable-console-print!)
  (sci/alter-var-root sci/print-fn (constantly *print-fn*))
  (sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*)))

;;;; Scratch

(comment

  (vars/push-thread-bindings {sci/file "hello"})


  )
