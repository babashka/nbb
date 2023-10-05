(ns nbb.impl.sci
  (:require [sci.core :as sci]
            [sci.ctx-store :as store]))

(def sns (sci/create-ns 'sci.core nil))

(defn ^:sci/macro copy-ns
  "Returns map of names to SCI vars as a result of copying public
  Clojure vars from ns-sym (a symbol). Attaches sci-ns (result of
  sci/create-ns) to meta. Copies :name, :macro :doc, :no-doc
  and :argslists metadata.

  Options:

  - :exclude: a seqable of names to exclude from the
  namespace. Defaults to none.

  - :copy-meta: a seqable of keywords to copy from the original var
  meta.  Use :all instead of a seqable to copy all. Defaults
  to [:doc :arglists :macro].

  - :exclude-when-meta: seqable of keywords; vars with meta matching
  these keys are excluded.  Defaults to [:no-doc :skip-wiki]

  The selection of vars is done at compile time which is mostly
  important for ClojureScript to not pull in vars into the compiled
  JS. Any additional vars can be added after the fact with sci/copy-var
  manually."
  ([_ _ ns-sym sci-ns] `(sci.core/copy-ns ~ns-sym ~sci-ns nil))
  ([_ _ ns-sym sci-ns opts]
     ;; this branch is hit by macroexpanding in JVM Clojure, not in the CLJS compiler
   (let [publics-map (sci/eval-form (store/get-ctx) (list 'ns-publics (list 'quote ns-sym)))
         publics-map (#'sci/process-publics publics-map opts)
         mf (#'sci/meta-fn (:copy-meta opts))
         publics-map (#'sci/exclude-when-meta
                      publics-map
                      meta
                      (fn [k]
                        (list 'quote k))
                      (fn [var m]
                        {:name (list 'quote (:name m))
                         :var var
                         :meta (list 'quote (mf m))})
                      (or (:exclude-when-meta opts)
                          [:no-doc :skip-wiki]))]
     `(sci.core/-copy-ns ~publics-map ~sci-ns))))

(defn ^:sci/macro copy-var
  "Copies contents from var `sym` to a new sci var. The value `ns` is an
  object created with `sci.core/create-ns`. If new-name is supplied, the
  copied var will be named new-name."
  ([_ _ sym ns]
   `(sci.core/copy-var ~sym ~ns nil))
  ([_ _ sym ns opts]
   (let [nm (:name opts)]
     `(let [ns# ~ns
            var# (var ~sym)
            val# (deref var#)
            m# (-> var# meta)
            name# (or ~nm (:name m#))
            new-m# {:doc (:doc m#)
                    :name name#
                    :arglists (:arglists m#)
                    :ns ns#}]
        (cond (:dynamic m#)
              (sci.core/new-dynamic-var name# val# new-m#)
              (or (:macro m#) (:sci/macro m#))
              (sci.core/new-macro-var name# val# new-m#)
              :else (sci.core/new-var name# val# new-m#))))))

(def sci-core-namespace
  (assoc (sci/copy-ns sci.core sns {:exclude [] #_[copy-ns]})
         'copy-ns (sci/copy-var copy-ns sns)
         '-copy-ns (sci/copy-var sci/-copy-ns sns)
         'copy-var (sci/copy-var copy-var sns)))
