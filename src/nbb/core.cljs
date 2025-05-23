(ns nbb.core
  (:refer-clojure :exclude [load-file time])
  (:require
   ["node:fs" :as fs]
   ["node:path" :as path]
   ["node:process" :as process]
   ["node:url" :as url]
   [babashka.cli]
   [cljs.tools.reader.reader-types]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [edamame.core]
   [goog.object :as gobj]
   [nbb.classpath :as cp]
   [nbb.common :refer [core-ns]]
   [nbb.error :as nbb.error]
   [nbb.impl.sci :as sci-cfg]
   [sci.core :as sci]
   [sci.ctx-store :as ctx]
   [sci.impl.unrestrict :refer [*unrestricted*]]
   [sci.impl.vars :as vars]
   [sci.lang]
   [shadow.esm :as esm])
  (:require-macros [nbb.macros :as macros]))

(set! *unrestricted* true)

(def await-counter 0)

(defn await [p]
  (if (and (not (nil? p))
           (unchecked-get p "then"))
    (do (set! await-counter (inc await-counter))
        (set! (.-__nbb_await_promise_result ^js p) true)
        p)
    p))

(defn await? [p]
  (and (not (nil? p))
       (unchecked-get p "then")
       (.-__nbb_await_promise_result ^js p)))

(def opts (atom nil))

(def repl-requires
  '[[clojure.repl :refer [apropos source dir doc find-doc]]
    [clojure.pprint :refer [pprint]]])

(def universe goog/global)

(def cwd (process/cwd))

(def command-line-args (sci/new-dynamic-var '*command-line-args* nil {:ns core-ns}))
(def warn-on-infer (sci/new-dynamic-var '*warn-on-infer* false {:ns core-ns}))

(def ctx (atom {}))

(def nbb-ns (sci/create-ns 'nbb.core nil))

(set! (.-import goog/global)
      (fn [what]
        ;; need to resolve based on the current file
        (esm/dynamic-import ((:resolve @ctx) what))))

(def loaded-modules (atom {}))

(declare load-file)
(declare handle-libspecs)

(def normalize-libname
  '{clojure.pprint cljs.pprint
    clojure.test cljs.test
    clojure.math cljs.math
    clojure.spec.alpha cljs.spec.alpha
    clojure.spec.gen.alpha cljs.spec.gen.alpha
    clojure.spec.test.alpha cljs.spec.test.alpha})

(def sci-find-ns (delay (sci/eval-form (ctx/get-ctx) 'find-ns)))

(defn load-module [m libname as refer rename libspecs opts]
  (-> (if (some? (@sci-find-ns libname))
        (js/Promise.resolve nil)
        (esm/dynamic-import m))
      (.then (fn [_module]
               (let [nlib libname]
                 (sci/binding [sci/ns (:ns opts)]
                   (when (and as nlib
                              (not= nlib libname))
                     (sci/eval-form (ctx/get-ctx)
                                    (list 'alias
                                          (list 'quote libname)
                                          (list 'quote nlib))))
                   (let [libname (or nlib libname)]
                     (when as
                       (sci/eval-form (ctx/get-ctx)
                                      (list 'alias
                                            (list 'quote as)
                                            (list 'quote libname))))
                     (when (seq refer)
                       (sci/eval-form (ctx/get-ctx)
                                      (list 'clojure.core/refer
                                            (list 'quote libname)
                                            :only (list 'quote refer)
                                            :rename (list 'quote rename))))))
                 (handle-libspecs (next libspecs) opts))))))

(def ^:private windows?
  (= "win32" process/platform))

(defn set-react! [mod]
  (set! ^js (.-nbb$internal$react goog/global) mod))

(defn load-react []
  (or (.-nbb$internal$react ^js goog/global)
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
        (set-react! mod))))

(defn set-react-dom! [mod]
  (set! ^js (.-nbb$internal$react-dom-server goog/global) mod))

(defn load-react-dom []
  (or ^js (.-nbb$internal$react-dom-server goog/global)
      (let [internal-name (symbol "nbb.internal.react-dom-server")
            mod
            (or (get @loaded-modules internal-name)
                (let [mod ((:require @ctx) "react-dom/server")]
                  (swap! loaded-modules assoc internal-name mod)
                  mod))]
        (set-react-dom! mod))))

(declare old-require)

(def feature-requires
  (macros/feature-requires))

(defn npm-lib-name []
  (macros/npm-lib-name))

(defn split-libname [libname]
  (str/split libname #"\$" 2))

(defn register-module [mod internal-name]
  (swap! loaded-modules assoc internal-name mod))

(defn debug [& xs]
  (binding [*print-fn* *print-err-fn*]
    (apply prn xs)))

(defn load-js-module [libname internal-name reload?]
  (let [react? (re-matches #"(.*:)?react(@.*)?" libname)
        react-dom? (re-matches #"(.*:)?react-dom(@.*)?/server" libname)]
    (-> (if (or (str/starts-with? (str libname) "jsr:")
                (str/starts-with? (str libname) "npm:")
                (str/starts-with? (str libname) "node:"))
          ;; fix for deno
          (js/Promise.resolve libname)
          (if-let [resolve (:resolve @ctx)]
            (js/Promise.resolve
             (try (resolve libname)
                  (catch :default _ ((.-resolve (:require @ctx)) libname))))
            (js/Promise.resolve ((.-resolve (:require @ctx)) libname))))
        (.then (fn [path]
                 (let [file-url (if (str/starts-with? (str path) "file:")
                                  path
                                  (when (and (or windows? reload?) (fs/existsSync path))
                                    (str (url/pathToFileURL path))))
                       path (if (and reload?
                                     ;; not "node:fs" etc
                                     file-url)
                              (str file-url "?uuid=" (random-uuid))
                              (or file-url path))]
                   (esm/dynamic-import path))))
        (.then (fn [mod]
                 (when react? (set-react! mod))
                 (when react-dom? (set-react-dom! mod))
                 (register-module mod internal-name)
                 mod))
        #_(.catch (fn [err]
                  (js/console.log err)
                  (throw err))))))

(defn munged->internal [munged]
  (symbol (str "nbb.internal." munged)))

(defn libname->internal-name [libname]
  (-> libname munge munged->internal))

(defn find-file-on-classpath [munged]
  (let [file (str/replace (str munged) #"\." "/")
        files [(str file ".cljs") (str file ".cljc") (str file ".clj")]
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
          current-ns (symbol current-ns-str)
          libname (normalize-libname libname libname)]
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
              (load-react-dom)
              (load-module "./nbb_reagent_dom_server.js" libname as refer rename libspecs ns-opts))
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
                (let [libname (if (str/starts-with? libname "./")
                                (path/resolve (path/dirname (or (:file ns-opts) "."))
                                              libname)
                                libname)
                      [libname properties*] (split-libname libname)
                      munged (munge libname)
                      properties (when properties* (.split properties* "."))
                      internal-name (munged->internal munged)
                      after-load
                      (fn [mod]
                        (let [internal-name
                              (if properties*
                                (symbol (str internal-name "$" properties*))
                                internal-name)]
                          (when as
                            (ctx/swap-ctx!
                             (fn [sci-ctx]
                               (-> sci-ctx
                                   (sci/add-class! internal-name mod)
                                   (sci/add-import! current-ns internal-name as))))))
                        (doseq [field refer]
                          (let [mod-field (gobj/get mod (str field))
                                ;; different namespaces can have different mappings
                                internal-subname (symbol (str internal-name "$" current-ns-str "$" field))
                                field (get rename field field)]
                            (ctx/swap-ctx!
                             (fn [sci-ctx]
                               (-> sci-ctx
                                   (sci/add-class! internal-subname mod-field)
                                   (sci/add-import! current-ns internal-subname field))))))
                        (handle-libspecs (next libspecs) ns-opts))
                      mod (let [reload? (contains? (:opts ns-opts) :reload)]
                            (js/Promise.resolve
                             (->
                              (or
                               ;; skip loading if module was already loaded
                               (and (not reload?)
                                    (some-> (get @loaded-modules internal-name)
                                            js/Promise.resolve))
                               (load-js-module libname internal-name reload?)
                               ;; else load module and register in loaded-modules under internal-name
                               )
                              (.then (fn [mod]
                                       (if properties
                                         (gobj/getValueByKeys mod properties)
                                         mod))))))]
                  (-> mod
                      (.then after-load)))
                :else
                ;; assume symbol
                (if (and (not (contains? (:opts ns-opts) :reload))
                         (sci/eval-form (ctx/get-ctx) (list 'clojure.core/find-ns (list 'quote libname))))
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
                               (sci/eval-form (ctx/get-ctx)
                                              (list 'clojure.core/alias
                                                    (list 'quote as)
                                                    (list 'quote libname))))
                             (when (seq refer)
                               (sci/eval-form (ctx/get-ctx)
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
        [require-forms other-forms] (reduce (fn [[require-forms other-forms] ns-form]
                                              (if (seq? ns-form)
                                                (let [fst (first ns-form)]
                                                  (cond (or (= :require fst)
                                                            (= :require-macros fst))
                                                        [(conj require-forms ns-form)
                                                         other-forms]
                                                        (= :use fst)
                                                        [(conj require-forms (map #(if (or (seq? %) (vector? %))
                                                                                     (replace {:only :refer} %)
                                                                                     %)
                                                                                  ns-form))
                                                         other-forms]
                                                        :else [require-forms
                                                               (conj other-forms ns-form)]))
                                                [require-forms
                                                 (conj other-forms ns-form)]))
                                            [[] []] ns-forms)
        ;; ignore all :require-macros for now
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form (ctx/get-ctx) (list 'do (list* 'ns ns-name other-forms) '*ns*)))
        libspecs (mapcat rest require-forms)
        ns-opts (into #{} (filter keyword? libspecs))
        libspecs (remove keyword? libspecs)
        opts (assoc opts :ns ns-obj)]
    (handle-libspecs libspecs (assoc opts :opts ns-opts))))

(defn eval-require [require-form]
  (let [args (rest require-form)
        args (remove keyword? args)
        libspecs (mapv #(sci/eval-form (ctx/get-ctx) %) args)
        sci-ns @sci/ns
        sci-file @sci/file]
    (handle-libspecs libspecs {:ns sci-ns
                               :file sci-file})))

(defn parse-next [reader]
  (sci/parse-next (ctx/get-ctx) reader
                  {:features #{:org.babashka/nbb
                               :cljs}}))

(declare eval-next)

(defn eval-simple [form opts]
  (sci/binding [sci/ns (:ns opts)
                sci/file (:file opts)]
    (sci/eval-form (ctx/get-ctx) form)))

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
                                (sci/eval-form (ctx/get-ctx) form))
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
  (ctx/swap-ctx! sci/merge-opts sci-opts))

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

(def sci-sym (delay (sci/eval-form (ctx/get-ctx) 'cljs.core/symbol)))
(def sci-var? (delay (sci/eval-form (ctx/get-ctx) 'cljs.core/var?)))

(defn ^:macro implements?* [_ _ psym x]
  (if-let [resolved (let [res (sci/resolve (ctx/get-ctx) psym)]
                      (if (@sci-var? res)
                        res
                        ;; workaround for resolve on `my.ns.Protocol` resolving to protocol map
                        (:name res)))]
    (let [psym (@sci-sym resolved)]
      ;; hardcoded implementation of implements? for js-interop destructure which
      ;; uses implements?
      (case psym
        clojure.core/ISeq (implements? ISeq x)
        clojure.core/INamed (implements? INamed x)
        clojure.core/IMeta (implements? IMeta x)
        (list 'cljs.core/instance? psym x)))
    false))

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

(def -invoked-file (atom nil))
(defn invoked-file
  "Return an absolute path for the file where nbb was invoked"
  []
  @-invoked-file)

(def main-ns (sci/create-ns 'clojure.main))

(ctx/reset-ctx!
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
                               'Atom (sci/copy-var Atom core-ns)
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
                               'PersistentVector PersistentVector
                               'IFn (sci/copy-var IFn core-ns)
                               'swap-vals! (sci/copy-var swap-vals! core-ns)
                               'reset-vals! (sci/copy-var reset-vals! core-ns)
                               'PersistentQueue (let [x PersistentQueue]
                                                  (gobj/set x "EMPTY" cljs.core/PersistentQueue.EMPTY)
                                                  x)
                               'demunge (sci/copy-var demunge core-ns)
                               'IWithMeta (sci/copy-var IWithMeta core-ns)
                               'IMeta (sci/copy-var IMeta core-ns)
                               'ISeq (sci/copy-var ISeq core-ns)
                               'INamed (sci/copy-var INamed core-ns)}
                'cljs.reader {'read-string (sci/copy-var edn/read-string (sci/create-ns 'cljs.reader))}
                'clojure.main {'repl-requires (sci/copy-var
                                               repl-requires
                                               main-ns)
                               'demunge (sci/copy-var demunge main-ns)}

                'nbb.core {'load-string (sci/copy-var load-string nbb-ns)
                           'load-file (sci/copy-var load-file nbb-ns)
                           'alter-var-root (sci/copy-var sci/alter-var-root nbb-ns)
                           'slurp (sci/copy-var slurp nbb-ns)
                           '*file* sci/file
                           'invoked-file invoked-file
                           'version (sci/copy-var version nbb-ns)
                           'await (sci/copy-var await nbb-ns)
                           'time (sci/copy-var time* nbb-ns)
                           'get-sci-ctx (sci/copy-var ctx/get-ctx nbb-ns)}
                'nbb.classpath {'add-classpath (sci/copy-var cp/add-classpath cp-ns)
                                'get-classpath (sci/copy-var cp/get-classpath cp-ns)}
                'nbb.error {'print-error-report (sci/copy-var print-error-report ens)}
                'goog.object goog-object-ns
                'edamame.core (sci/copy-ns edamame.core (sci/create-ns 'edamame.core))
                'babashka.cli cli-namespace
                'sci.core sci-cfg/sci-core-namespace}
   :classes {'js universe :allow :all
             'goog.object (clj->js goog-object-ns)
             'ExceptionInfo ExceptionInfo
             'Math js/Math}}))

(sci/enable-unrestricted-access!)

(def old-require (sci/eval-form (ctx/get-ctx) 'require))

(def ^:dynamic *old-require* false)

(swap! (:env (ctx/get-ctx)) assoc-in
       [:namespaces 'clojure.core 'require]
       (fn [& args]
         (if *old-require*
           (apply old-require args)
           (await (.then (identity ;; with-async-bindings {sci/file @sci/file}
                          (let [opts (into #{} (filter keyword? args))
                                args  (remove keyword? args)]
                            (handle-libspecs args {:ns @sci/ns
                                                   :file @sci/file
                                                   :opts opts})))
                         (fn [_]))))))

(def ^:dynamic *file* sci/file) ;; make clj-kondo+lsp happy

(defn init []
  (enable-console-print!)
  (sci/alter-var-root sci/print-fn (constantly *print-fn*))
  (sci/alter-var-root sci/print-err-fn (constantly *print-err-fn*)))

;;;; Scratch

(comment

  (vars/push-thread-bindings {sci/file "hello"}))
