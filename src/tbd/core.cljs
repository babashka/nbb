(ns tbd.core
  (:require [sci.core :as sci]
            [shadow.esm :as esm]))

(def universe goog/global)

(def cwd (.cwd js/process))

;; hack from  https://swizec.com/blog/making-a-node-cli-both-global-and-local/
#_(defn patched-import [s]
  (if (str/starts-with? s ".")
    (esm/dynamic-import (str/join "/" [cwd s]))
    (let [path (str/join "/" [cwd "node_modules" s])]
      (-> (esm/dynamic-import path)
          (.catch
           (fn [_]
             (esm/dynamic-import s)))))))
;; also see https://github.com/thheller/shadow-cljs/blob/master/packages/shadow-cljs/cli/runner.js

(set! (.-import universe) esm/dynamic-import)

(def sci-ctx (atom (sci/init {:namespaces {'clojure.core {'prn prn 'println println}}
                              :classes {'js universe :allow :all}})))

(defn eval_code [code]
  (let [reader (sci/reader code)]
    (try
      (loop [result nil]
        (let [next-val (sci/parse-next @sci-ctx reader)]
          (if-not (= :sci.core/eof next-val)
            (let [result (sci/eval-form @sci-ctx next-val)]
              (recur result))
            result)))
      (catch :default e
        (prn (str e))))))

(defn register-plugin! [plug-in-name sci-opts]
  plug-in-name ;; unused for now
  (swap! sci-ctx sci/merge-opts sci-opts))
