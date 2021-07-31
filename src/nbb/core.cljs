(ns nbb.core
  (:require [clojure.string :as str]
            [sci.core :as sci]
            [shadow.esm :as esm]))

(def universe goog/global)

(def cwd (.cwd js/process))

(set! (.-import universe) esm/dynamic-import)
#_(set! (.-require universe) (js/nbb.core._require_))

(def sci-ctx (atom (sci/init {:namespaces {'clojure.core {'prn prn 'println println}}
                              :classes {'js universe :allow :all}})))

(def last-ns (atom @sci/ns))

(defn dynamic-import
  ;; "This is a workaround for loading local .js and .json file relative to the script itself."
  [s]
  (esm/dynamic-import s)
  #_(if (str/starts-with? s ".")
    (.catch (esm/dynamic-import (str/join "/" [cwd s]))
            (fn [_]
              (esm/dynamic-import s)))
    (esm/dynamic-import s)))

(declare eval-expr)

(defn handle-libspecs [ns-obj reader libspecs]
  (if libspecs
    (let [fst (first libspecs)
          [libname & opts] fst
          opts (apply hash-map opts)
          as (:as opts)
          default (:default opts)
          refer (:refer opts)]
      (case libname
        ;; built-ins
        (reagent.core reagent.dom reagent.dom.server)
        (-> (esm/dynamic-import "./nbb_reagent.js")
            (.then (fn [_mod]
                     (when as
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname)))))
                     (handle-libspecs ns-obj reader (next libspecs)))))
        ;; default
        (-> (dynamic-import libname)
            (.then (fn [mod]
                     (when as
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'def as mod))))
                     (when default
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'def default (.-default mod)))))
                     (doseq [field refer]
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'def field (aget mod (str field))))))
                     (handle-libspecs ns-obj reader (next libspecs)))))))
    (eval-expr ns-obj reader)))

(defn eval-ns-form [reader ns-form]
  ;; the parsing is still very crude, we only support a subset of the ns form
  (let [[_ns ns-name requires] ns-form
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form @sci-ctx (list 'do (list 'ns ns-name) '*ns*)))
        _ (reset! last-ns ns-obj)
        [_require & libspecs] requires]
    (handle-libspecs ns-obj reader libspecs)))

(defn eval-require [reader require-form]
  (let [args (rest require-form)
        libspecs (sci/binding [sci/ns @last-ns]
                       (mapv #(sci/eval-form @sci-ctx %) args))]
    (handle-libspecs @last-ns reader libspecs)))

(defn eval-expr
  "Evaluates top level forms asynchronously. Returns promise of last value."
  [prev-val reader]
  (let [next-val (sci/parse-next @sci-ctx reader)]
    (if-not (= :sci.core/eof next-val)
      (if (seq? next-val)
        (let [fst (first next-val)]
          (cond (= 'ns fst)
                (eval-ns-form reader next-val)
                (= 'require fst)
                (eval-require reader next-val)
                :else
                (let [result (sci/binding [sci/ns @last-ns]
                               (sci/eval-form @sci-ctx next-val))]
                  (recur result reader))))
        (let [result (sci/binding [sci/ns @last-ns]
                       (sci/eval-form @sci-ctx next-val))]
          (recur result reader)))
      ;; wrap normal value in promise
      (js/Promise.resolve prev-val))))

(defn eval-code [code]
  (let [reader (sci/reader code)]
    (eval-expr nil reader)))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))

(defn init [])
