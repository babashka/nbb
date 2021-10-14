(ns nbb.impl.console-repl
  (:require
   ["path" :as path]
   ["readline" :as readline]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.core :as nbb]
   [sci.core :as sci])
  (:require-macros [nbb.macros :as macros]))

(defn create-rl []
  (.createInterface
   readline #js {:input js/process.stdin
                 :output js/process.stdout}))


(def pending (atom ""))

(declare input-loop)

(def last-ns (atom @sci/ns))

(defn on-line [^js rl]
  (.on rl "line" (fn [input]
                   (-> (macros/with-async-bindings {sci/ns @last-ns}
                         (-> (nbb/eval-expr nil (sci/reader (str @pending input)))
                             (.then (fn [v]
                                      [v (sci/eval-form @nbb/sci-ctx '*ns*)]))))
                       (.then (fn [[val ns]]
                                (reset! pending "")
                                (reset! last-ns ns)
                                (.close rl)
                                (prn val)
                                (input-loop)))
                       (.catch (fn [e]
                                 (let [m (ex-message e)]
                                   (if (str/includes? m "EOF while reading")
                                     (swap! pending str input "\n")
                                     (throw e)))))))))

(defn input-loop []
  (let [rl (create-rl)
        _ (on-line rl)
        _ (.setPrompt rl (str @last-ns "> "))]
    (.prompt rl)))

(defn init []
  (api/init-require (path/resolve "script.cljs"))
  (input-loop))
