(ns nbb.impl.fs
  {:no-doc true}
  (:require [babashka.fs]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def fns (sci/create-ns 'babashka.fs nil))

;; babashka.fs/with-temp-dir is a compile-time macro, so copy-ns can't copy it as
;; a runtime var. Reinterpret it here as an SCI macro that expands to the copied
;; create-temp-dir / delete-tree vars.
(defn ^:macro with-temp-dir
  "Evaluates body with `temp-dir` bound to the result of `(create-temp-dir opts)`.

  By default, the `temp-dir` will be removed with `delete-tree` on exit from the
  scope.

  Options:
  * see `create-temp-dir` for options that control directory creation
  * `:keep` - if `true` does not delete the directory on exit from macro scope."
  [_ _ [temp-dir opts & more] & body]
  (assert (empty? more) "with-temp-dir binding takes at most 2 forms")
  (assert (symbol? temp-dir) "with-temp-dir needs a symbol to bind")
  `(let [opts# ~opts
         ~temp-dir (babashka.fs/create-temp-dir opts#)]
     (try
       ~@body
       (finally
         (when-not (:keep opts#)
           (babashka.fs/delete-tree ~temp-dir {:force true}))))))

(def fs-namespace
  (assoc (sci/copy-ns babashka.fs fns {:exclude [with-temp-dir]})
         'with-temp-dir (sci/copy-var with-temp-dir fns)))

(defn init []
  (nbb/register-plugin!
   ::fs
   {:namespaces {'babashka.fs fs-namespace}}))
