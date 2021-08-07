(ns nbb.core
  (:refer-clojure :exclude [load-file])
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [sci.core :as sci]
   [shadow.esm :as esm]))

;; workaround for import$fs not defined
(def fs (volatile! nil))
(def path (volatile! nil))

(def universe goog/global)

(def cwd (.cwd js/process))

(def core-ns (sci/create-ns 'clojure.core nil))

(def command-line-args (sci/new-dynamic-var '*command-line-args* nil {:ns core-ns}))

(def ctx (atom {}))

;; (declare eval-expr load-string slurp load-file)

(def nbb-ns (sci/create-ns 'nbb.core nil))

(def sci-ctx (atom nil))

(def last-ns (atom @sci/ns))

(set! (.-import goog/global) esm/dynamic-import)

(def loaded-modules (atom {}))

(defn import-via* [path]
  (-> (esm/dynamic-import "import-meta-resolve")
      (.then (fn [mod]
               (let [resolve (.-resolve mod)]
                 (fn [lib]
                   (.then (resolve lib (str "file://" path))
                          (fn [resolved]
                            (esm/dynamic-import resolved)))))))))

(def import-via (memoize import-via*))

(defn handle-libspecs [ns-obj libspecs]
  (if (seq libspecs)
    (let [fst (first libspecs)
          [libname & opts] fst
          opts (apply hash-map opts)
          as (:as opts)
          refer (:refer opts)]
      (case libname
        ;; built-ins
        (reagent.core reagent.dom reagent.dom.server)
        (let [script-dir (:script-dir @ctx)]
          ;; TODO: remove import-via, it doesn't work, Reagent will still load nbb's optional react itself
          (-> (-> (import-via "react" script-dir) ;; resolve react from script location
                  (.then (fn [_react]
                           ;; then load local reagent module
                           (esm/dynamic-import "./nbb_reagent.js")))
                  (.then (fn [_reagent]
                           (when as
                             (sci/binding [sci/ns ns-obj]
                               (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname)))))
                           (handle-libspecs ns-obj (next libspecs)))))))
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
                current-ns (symbol (str ns-obj))]
            (sci/binding [sci/ns ns-obj]
              (when as
                (sci/binding [sci/ns ns-obj]
                  (swap! sci-ctx sci/merge-opts {:classes {internal-name mod}})
                  ;; HACK, we register the alias as a reference to the class
                  ;; via :imports we should expose this functionality in SCI
                  ;; itself as this relies on the internal representation of
                  (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports as] internal-name)))
              (doseq [field refer]
                (let [mod-field (gobj/get mod (str field))
                      internal-subname (str internal-name "$" field)]
                  (swap! sci-ctx sci/merge-opts {:classes {internal-subname mod-field}})
                  ;; Repeat hack from above
                  (swap! (:env @sci-ctx) assoc-in [:namespaces current-ns :imports field] internal-subname))))
            (recur ns-obj (next libspecs)))
          ;; assume symbol
          (do (sci/binding [sci/ns ns-obj]
                (sci/eval-form @sci-ctx (list 'require (list 'quote fst))))
              (recur ns-obj (next libspecs))))))
    (js/Promise.resolve ns-obj)))

(defn eval-ns-form [ns-form]
  ;; the parsing is still very crude, we only support a subset of the ns form
  ;; and ignore everything but (:require clauses)
  (let [[_ns ns-name & ns-forms] ns-form
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form @sci-ctx (list 'do (list 'ns ns-name) '*ns*)))
        _ (reset! last-ns ns-obj)
        require-forms (filter (fn [ns-form]
                                (and (seq? ns-form)
                                     (= :require (first ns-form))))
                              ns-forms)
        libspecs (mapcat (fn [require-form]
                           (rest require-form))
                         require-forms)]
    (handle-libspecs ns-obj libspecs)))

(defn eval-require [require-form]
  (let [args (rest require-form)
        libspecs (sci/binding [sci/ns @last-ns]
                   (mapv #(sci/eval-form @sci-ctx %) args))]
    (handle-libspecs @last-ns libspecs)))

(defn eval-expr
  "Evaluates top level forms asynchronously. Returns promise of last value."
  [prev-val reader]
  (let [next-val (try (sci/binding [sci/ns @last-ns]
                        (sci/parse-next @sci-ctx reader))
                      (catch :default e
                        (js/Promise.resolve e)))]
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
                (let [result (try (sci/binding [sci/ns @last-ns]
                                    ;; assume synchronous execution, so binding is OK.
                                    (sci/eval-form @sci-ctx next-val))
                                  (catch :default e
                                    (js/Promise.resolve e)))]
                  (recur result reader))))
        ;; assume synchronous execution, so binding is OK.
        (let [result (try (sci/binding [sci/ns @last-ns]
                            (sci/eval-form @sci-ctx next-val))
                          (catch :default e
                            (js/Promise.resolve e)))]
          (recur result reader)))
      ;; wrap normal value in promise
      (js/Promise.resolve prev-val))))

(defn eval-string* [s]
  (let [reader (sci/reader s)]
    (eval-expr nil reader)))

(defn load-string
  "Asynchronously parses and evaluates string s. Returns promise."
  [s]
  (let [ns @last-ns]
    (-> (eval-string* s)
        (.finally (fn []
                    ;; restore ns
                    (reset! last-ns ns))))))

(defn slurp
  "Synchronously returns string from file f."
  [f]
  (str ((.-readFileSync ^js @fs) f)))

(defn load-file
  [f]
  (-> f slurp load-string))

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
                                  'load-file (sci/copy-var load-file nbb-ns)}}
          :classes {'js universe :allow :all}
          :load-fn (fn [{:keys [namespace]}]
                     (let [munged (munge namespace)
                           file (str/replace (str munged) #"\." "/")
                           file (str file ".cljs")
                           dirs (-> @ctx :classpath :dirs)
                           resolve (.-resolve ^js @path)
                           exists (.-existsSync ^js @fs)
                           the-file (reduce (fn [_ dir]
                                              (let [f (resolve dir file)]
                                                (when (exists f)
                                                  (reduced f)))) nil dirs)]
                       {:source (slurp the-file)
                        :file the-file}))}))

(defn init [])
