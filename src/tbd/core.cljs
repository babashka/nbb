(ns tbd.core
  (:require [sci.core :as sci]
            [shadow.esm :as esm]))

(def universe goog/global)

(def cwd (.cwd js/process))

(set! (.-import universe) esm/dynamic-import)

(def sci-ctx (atom (sci/init {:namespaces {'clojure.core {'prn prn 'println println}}
                              :classes {'js universe :allow :all}})))

(def last-ns (atom @sci/ns))

(defn handle-libspecs [ns-obj cb libspecs]
  (if libspecs
    (let [fst (first libspecs)
          [libname & opts] fst
          opts (apply hash-map opts)
          as (:as opts)
          default (:default opts)]
      (case libname
        ;; built-ins
        (reagent.core reagent.dom reagent.dom.server)
        (-> (esm/dynamic-import "./tbd_reagent.js")
            (.then (fn [_mod]
                     (when as
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'alias (list 'quote as) (list 'quote libname)))))
                     (handle-libspecs ns-obj cb (next libspecs)))))
        ;; default
        (-> (esm/dynamic-import libname)
            (.then (fn [mod]
                     (when as
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'def as mod))))
                     (when default
                       (sci/binding [sci/ns ns-obj]
                         (sci/eval-form @sci-ctx (list 'def default (.-default mod)))))
                     (handle-libspecs ns-obj cb (next libspecs)))))))
    (cb ns-obj)))

(defn eval-ns-form [reader ns-form cb]
  ;; the parsing is still very crude, we only support a subset of the ns form
  (let [[_ns ns-name requires] ns-form
        ns-obj (sci/binding [sci/ns @sci/ns]
                 (sci/eval-form @sci-ctx (list 'do (list 'ns ns-name) '*ns*)))
        _ (reset! last-ns ns-obj)
        [_require & libspecs] requires]
    (handle-libspecs ns-obj cb libspecs)))

(defn read-next [reader result]
  (let [next-val (sci/parse-next @sci-ctx reader)]
    (if-not (= :sci.core/eof next-val)
      (if (and (seq? next-val)
               (= 'ns (first next-val)))
        (eval-ns-form reader next-val #(read-next reader %))
        (recur reader (sci/binding [sci/ns @last-ns]
                        (sci/eval-form @sci-ctx next-val))))
      result)))

(defn eval_code [code]
  (let [reader (sci/reader code)]
    (try
      (loop [result nil
             init? true]
        (let [next-val (sci/parse-next @sci-ctx reader)]
          (if-not (= :sci.core/eof next-val)
            (if (and init?
                     (seq? next-val)
                     (= 'ns (first next-val)))
              (eval-ns-form reader next-val #(read-next reader %))
              (let [result (sci/binding [sci/ns @last-ns]
                             (sci/eval-form @sci-ctx next-val))]
                (recur result false)))
            result)))
      (catch :default e
        (.log js/console e)))))

(defn register-plugin! [_plug-in-name sci-opts]
  (swap! sci-ctx sci/merge-opts sci-opts))
