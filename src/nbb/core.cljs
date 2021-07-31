(ns nbb.core
  (:require
   [clojure.string :as str]
   [sci.core :as sci]
   #_[shadow.esm :as esm]))

(def universe goog/global)

(def cwd (.cwd js/process))

#_(set! (.-import universe) esm/dynamic-import)

(def sci-ctx (atom (sci/init {:namespaces {'clojure.core {'prn prn 'println println}}
                              :classes {'js universe :allow :all}})))

(def last-ns (atom @sci/ns))

#_(defn dynamic-import
  ;; "This is a workaround for loading local .js and .json file relative to the script itself."
  [s]
  (esm/dynamic-import s)
  #_(if (str/starts-with? s ".")
    (.catch (esm/dynamic-import (str/join "/" [cwd s]))
            (fn [_]
              (esm/dynamic-import s)))
    (esm/dynamic-import s)))

(declare eval-expr)

(defn handle-libspecs [ns-obj libspecs require]
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
        (let [_ (require "nbb/out/nbb_reagent")]
          (when as
            (sci/binding [sci/ns ns-obj]
              (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname)))))
          (handle-libspecs ns-obj(next libspecs) require))
        ;; default
        (if (string? libname)
          ;; TODO: parse properties
          (let [[libname properties] (str/split libname #"\\$")
                properties nil]
            (prn :require libname)
            (let [mod (require libname)]
              (when as
                (sci/binding [sci/ns ns-obj]
                  (sci/eval-form @sci-ctx (list 'def as mod))))
              (when default
                (sci/binding [sci/ns ns-obj]
                  (sci/eval-form @sci-ctx (list 'def default (.-default mod)))))
              (sci/binding [sci/ns ns-obj]
                (doseq [field refer]
                  (sci/eval-form @sci-ctx (list 'def field (aget mod (str field)))))))
            (recur ns-obj (next libspecs) require))
          ;; assume symbol
          (do (sci/binding [sci/ns ns-obj]
                (sci/eval-form @sci-ctx (list 'require (list 'quote fst))))
              (recur ns-obj (next libspecs) require)))))
    ns-obj))

(defn eval-ns-form [ns-form require]
  ;; the parsing is still very crude, we only support a subset of the ns form
  (let [[_ns ns-name requires] ns-form
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form @sci-ctx (list 'do (list 'ns ns-name) '*ns*)))
        _ (reset! last-ns ns-obj)
        [_require & libspecs] requires]
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
                (recur (eval-ns-form next-val require) reader require)
                (= 'require fst)
                (recur (eval-require next-val require) reader require)
                :else
                (let [result (sci/binding [sci/ns @last-ns]
                               (sci/eval-form @sci-ctx next-val))]
                  (recur result reader require))))
        (let [result (sci/binding [sci/ns @last-ns]
                       (sci/eval-form @sci-ctx next-val))]
          (recur result reader require)))
      ;; wrap normal value in promise
      prev-val)))

(defn eval-code [code require]
  (let [reader (sci/reader code)]
    (eval-expr nil reader require)))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))

(defn init [])
