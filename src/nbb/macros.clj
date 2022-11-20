(ns nbb.macros
  (:refer-clojure :exclude [time])
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]))

(defmacro with-async-bindings [m & body]
  `(do (sci.impl.vars/push-thread-bindings ~m)
       (.finally (do ~@body)
                 (fn []
                   (sci.impl.vars/pop-thread-bindings)))))

(defmacro get-in-package-json [k]
  (get (json/read-str (slurp "package.json") :key-fn keyword) k))

(defmacro cli-name
  []
  (or (System/getenv "NBB_CLI_NAME") "nbb"))

(defmacro npm-lib-name
  []
  (or (System/getenv "NBB_NPM_LIB_NAME") "nbb"))

(defmacro feature-requires []
  (let [;; all nbb_features.edn files on the classpath:
        configs (enumeration-seq
                 (.getResources (.getContextClassLoader (Thread/currentThread))
                                "nbb_features.edn"))
        m (->> configs
               (mapcat (comp edn/read-string slurp str))
               (mapcat (fn [{:keys [namespaces js]}]
                         (mapv (fn [n] [n js]) namespaces)))
               (into '{promesa.core "./nbb_promesa.js"
                       applied-science.js-interop "./nbb_js_interop.js"
                       cljs-bean.core "./nbb_cljs_bean.js"
                       cljs.pprint "./nbb_pprint.js"
                       clojure.pprint "./nbb_pprint.js"
                       cljs.test "./nbb_test.js"
                       clojure.test "./nbb_test.js"
                       nbb.repl "./nbb_repl.js"
                       clojure.tools.cli "./nbb_tools_cli.js"
                       goog.string "./nbb_goog_string.js"
                       goog.string.format "./nbb_goog_string.js"
                       goog.crypt "./nbb_goog_crypt.js"
                       cognitect.transit "./nbb_transit.js"
                       clojure.data "./nbb_data.js"
                       cljs.math "./nbb_math.js"
                       clojure.math "./nbb_math.js"
                       clojure.zip "./nbb_zip.js"
                       nbb.nrepl-server "./nbb_nrepl_server.js"}))]
    (list 'quote m)))

(defmacro time
  "Async version of time."
  [expr]
  `(let [start# (cljs.core/system-time)
         ret# ~expr
         ret# (js/Promise.resolve ret#)]
     (nbb.core/await
      (.then ret# (fn [v#]
                    (prn (cljs.core/str "Elapsed time: "
                                        (.toFixed (- (cljs.core/system-time) start#) 6)
                                        " msecs"))
                    v#)))))
