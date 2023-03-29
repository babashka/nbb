(ns nbb.common
  (:require
   [sci.core :as sci]
   [clojure.string :as str]))

(def core-ns (sci/create-ns 'clojure.core nil))

(defn main-expr [main-fn]
  (let [main-fn (symbol main-fn)
        main-fn (if (simple-symbol? main-fn)
                  (symbol (str main-fn) "-main")
                  main-fn)
        ns (namespace main-fn)
        expr (str/replace "(require '$1) (apply $2 *command-line-args*)"
                          #"\$(\d)"
                          (fn [match]
                            (case (second match)
                              "1" ns
                              "2" main-fn)))]
    expr))
