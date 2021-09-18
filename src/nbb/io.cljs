(ns nbb.io
  (:refer-clojure :exclude [prn print println with-out-str])
  (:require
   [cljs.core :as c]
   [goog.string]
   [nbb.common :refer [core-ns]]
   [sci.core :as sci]))

(def print-fn (sci/copy-var *print-fn* core-ns))
(def print-newline (sci/copy-var *print-newline* core-ns))

(defn println [& objs]
  (binding [*print-fn* @print-fn
            *print-newline* @print-newline]
    (apply c/println objs)))

(defn prn [& objs]
  (binding [*print-fn* @print-fn
            *print-newline* @print-newline]
    (apply c/prn objs)))

(defn print [& objs]
  (binding [*print-fn* @print-fn]
    (apply c/print objs)))

(defn ^:macro with-out-str
  "Evaluates exprs in a context in which *print-fn* is bound to .append
  on a fresh StringBuffer.  Returns the string created by any nested
  printing calls."
  [_ _ & body]
  `(let [sb# (goog.string/StringBuffer.)]
     (binding [cljs.core/*print-newline* true
               cljs.core/*print-fn* (fn [x#] (.append sb# x#))]
       ~@body)
     (cljs.core/str sb#)))
