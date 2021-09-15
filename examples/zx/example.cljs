(ns example
  {:clj-kondo/config '{:lint-as {promesa.core/let clojure.core/let}}}
  (:require ["zx$default" :as zx]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn $
  "Wrapper for zx/$ with automatic conversion of input and output to
  CLJS data structures. Supports setting verbosity during execution of
  command, which zx doesn't support out of the box."
  [opts & args]
  (p/let [[opts args] (if (map? opts)
                        [opts args]
                        [nil (cons opts args)])
          old-verbose (.-verbose zx/$)
          {:keys [verbose] :or {verbose old-verbose}} opts
          _ (set! (.-verbose zx/$) verbose)
          res (-> (zx/$ (clj->js (into-array args)))
                  (.finally (fn []
                              (set! (.-verbose zx/$) old-verbose))))]
    {:stdout (str/trim (.-stdout res))}))

(p/let [{branch :stdout} ($ {:verbose false} "git branch --show-current")
        _ ($ "sleep 1; echo 1")
        _ ($ "sleep 2; echo 2")
        _ ($ "sleep 3; echo 3")]
  (println "The branch was" (pr-str branch)))

;; output:

;; $ sleep 1; echo 1
;; 1
;; $ sleep 2; echo 2
;; 2
;; $ sleep 3; echo 3
;; 3
;; The branch was "main"
