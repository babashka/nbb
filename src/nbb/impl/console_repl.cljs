(ns nbb.impl.console-repl
  (:require
   ["readline" :as readline]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.core :as nbb]
   [sci.core :as sci]))

(defn create-rl []
  (.createInterface
   readline #js {:input js/process.stdin
                 :output js/process.stdout}))


(def pending (atom ""))

(declare input-loop)

(defn on-line [rl]
  (.on rl "line" (fn [input]
                   (let [rdr (sci/reader (str @pending input))
                         next-val (try (sci/parse-next @nbb/sci-ctx rdr)
                                       (catch :default e
                                         (let [m (ex-message e)]
                                           (if (str/includes? m "EOF while reading")
                                             ::eof-while-reading
                                             (throw e)))))]
                     (if (= ::eof-while-reading next-val)
                       (swap! pending str input "\n")
                       (do
                         (reset! pending "")
                         
                         (.close rl)
                         (input-loop)))))))

(defn input-loop []
  (let [rl (create-rl)
        _ (on-line rl)]
    (.prompt rl)))

(defn init []
  (prn :hello)
  (input-loop))
