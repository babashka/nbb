(ns tbd.core
  (:require [clojure.string :as str]
            [sci.core :as sci]))

(def universe goog/global)

(def cwd (.cwd js/process))

;; hack from  https://swizec.com/blog/making-a-node-cli-both-global-and-local/
(let [normal-require js/require]
  (defn patched-require [s]
    (if (str/starts-with? s ".")
      (normal-require (str/join "/" [cwd s]))
      (let [path (str/join "/" [cwd "node_modules" s])]
        (try (normal-require path)
             (catch :default _e
               (normal-require s)))))))

(set! (.-require universe) patched-require)

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

(prn :dude)
