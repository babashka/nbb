(ns nbb.impl.repl
  (:require
   ["net" :as net]
   ["path" :as path]
   ["readline" :as readline]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.core :as nbb]
   [nbb.impl.common :as common]
   [sci.core :as sci])
  (:require-macros [nbb.macros :as macros]))

(defn create-rl []
  (.createInterface
   readline #js {:input js/process.stdin
                 :output js/process.stdout}))


(def pending (atom ""))

(declare input-loop)

(def last-ns (atom @sci/ns))

(defn input-handler
  [socket rl input]
  (-> (macros/with-async-bindings {sci/ns @last-ns}
        (-> (nbb/eval-expr nil (sci/reader (str @pending input)) {:wrap vector})
            (.then (fn [v]
                     [(first v) (sci/eval-form @nbb/sci-ctx '*ns*)]))))
      (.then (fn [[val ns]]
               (reset! pending "")
               (reset! last-ns ns)
               (.close rl)
               (if socket
                 (.write socket (prn-str val))
                 (prn val))
               (input-loop socket)))
      (.catch (fn [e]
                (let [m (ex-message e)]
                  (if (str/includes? m "EOF while reading")
                    (swap! pending str input "\n")
                    (throw e)))))))

(defn on-line [^js rl socket]
  (.on rl "line" #(input-handler socket rl %)))

(defn create-socket-rl [socket]
  (.createInterface
   readline #js {:input socket
                 :output socket}))

(defn input-loop [socket]
  (let [rl (if socket
             (create-socket-rl socket)
             (create-rl))
        _ (on-line rl socket)
        _ (.setPrompt rl (str @last-ns "=> "))]
    (.prompt rl)))

(defn on-connect [socket]
  (let [rl (create-socket-rl socket)]
    (on-line rl socket))
  (.setNoDelay ^net/Socket socket true)
  (.on ^net/Socket socket "close"
       (fn [_had-error?]
         (println "Client closed connection."))))

(defn init []
  (api/init-require (path/resolve "script.cljs"))
  (if-let [port (:port @common/opts)]
    (let [srv (net/createServer
               (fn [socket]
                 (on-connect socket)))]
      (.listen srv port "127.0.0.1"
               (fn []
                 (let [addr (-> srv (.address))
                       port (-> addr .-port)
                       host (-> addr .-address)]
                   (println (str "Socket REPL listening on port "
                                 port " on host " host))))))
    (input-loop nil)))
