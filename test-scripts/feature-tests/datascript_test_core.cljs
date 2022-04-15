(ns datascript.test.core
  "This is a minimal version of datascript.test.core that works with nbb tests"
  (:require [cljs.test :as t]
            [datascript.core :as d]))

(defmethod t/assert-expr 'thrown-msg? [menv msg form]
  (let [[_ match & body] form]
    `(try
       ~@body
       (t/do-report {:type :fail, :message ~msg, :expected '~form, :actual nil})
       (catch :default e#
         (let [m# (.-message e#)]
           (if (= ~match m#)
             (t/do-report {:type :pass, :message ~msg, :expected '~form, :actual e#})
             (t/do-report {:type :fail, :message ~msg, :expected '~form, :actual e#}))
           e#)))))

(defn all-datoms [db]
  (into #{} (map (juxt :e :a :v)) (d/datoms db :eavt)))
