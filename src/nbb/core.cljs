(ns nbb.core
  (:require
   [clojure.string :as str]
   [goog.object :as gobj]
   [sci.core :as sci]
   [shadow.esm :as esm]))

(def universe goog/global)

(def cwd (.cwd js/process))

(def core-ns (sci/create-ns 'clojure.core nil))

(def command-line-args (sci/new-dynamic-var '*command-line-args* nil {:ns core-ns}))

(def sci-ctx
  (atom
   (sci/init
    {:namespaces {'clojure.core {'prn prn
                                 'print print
                                 'println println
                                 '*command-line-args* command-line-args}}
     :classes {'js universe :allow :all}})))

(def last-ns (atom @sci/ns))

(declare eval-expr)

(set! (.-import goog/global) esm/dynamic-import)

(def loaded-modules (atom {}))

(defn handle-libspecs [ns-obj libspecs require]
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
            (.then (fn [_mod]
                     (when as
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname)))))
                     (handle-libspecs ns-obj (next libspecs) require))))
        ;; default
        (if (string? libname)
          ;; TODO: parse properties
          (let [[libname _properties] (str/split libname #"\\$")
                internal-name (symbol (str "nbb.internal." (munge libname)))
                mod (or
                     ;; skip loading if module was already loaded
                     (get @loaded-modules internal-name)
                     ;; else load module and register in loaded-modules under internal-name
                     (let [mod (require libname)]
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
            (recur ns-obj (next libspecs) require))
          ;; assume symbol
          (do (sci/binding [sci/ns ns-obj]
                (sci/eval-form @sci-ctx (list 'require (list 'quote fst))))
              (recur ns-obj (next libspecs) require)))))
    (js/Promise.resolve ns-obj)))

(defn eval-ns-form [ns-form require]
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
    (handle-libspecs ns-obj libspecs require)))

(defn eval-require [require-form require]
  (let [args (rest require-form)
        libspecs (sci/binding [sci/ns @last-ns]
                       (mapv #(sci/eval-form @sci-ctx %) args))]
    (handle-libspecs @last-ns libspecs require)))

(defn eval-expr
  "Evaluates top level forms asynchronously. Returns promise of last value."
  [prev-val reader require]
  (let [next-val (sci/parse-next @sci-ctx reader)]
    (if-not (= :sci.core/eof next-val)
      (if (seq? next-val)
        (let [fst (first next-val)]
          (cond (= 'ns fst)
                ;; async
                (.then (eval-ns-form next-val require)
                       (fn [ns-obj]
                         (eval-expr ns-obj reader require)))
                (= 'require fst)
                ;; async
                (.then (eval-require next-val require)
                       (fn [_]
                         (eval-expr nil reader require)))
                :else
                (let [result (sci/binding [sci/ns @last-ns]
                               (sci/eval-form @sci-ctx next-val))]
                  (recur result reader require))))
        (let [result (sci/binding [sci/ns @last-ns]
                       (sci/eval-form @sci-ctx next-val))]
          (recur result reader require)))
      ;; wrap normal value in promise
      (js/Promise.resolve prev-val))))

(defn eval-code [code require]
  (let [reader (sci/reader code)]
    (.catch (eval-expr nil reader require)
            (fn [err]
              (.error js/console (str err))))))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))

(defn init [])
