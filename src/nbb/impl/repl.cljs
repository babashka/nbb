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


(def pending-input (atom ""))
(def pending-forms (atom ""))

(declare input-loop)

(def last-ns (atom @sci/ns))

(defn input-handler
  [socket rl input]
  (swap! pending-input str input "\n")
  (when-not (str/blank? @pending-input)
    (let [rdr (sci/reader @pending-input)
          the-val (try (nbb/parse-next rdr)
                       (catch :default e
                         (if (str/includes? (ex-message e) "EOF while reading")
                           ::eof-while-reading
                           (throw e))))]
      (when-not (= ::eof-while-reading the-val)
        (let [line (sci/get-line-number rdr)
              col (sci/get-column-number rdr)
              lines (str/split-lines @pending-input)
              [line & lines] (drop (dec line) lines)
              edited (subs line col)]
          (reset! pending-input "")
          (reset! pending-forms (str/join "\n" (cons edited lines))))
        (when-not (= :sci.core/eof the-val)
          (macros/with-async-bindings {sci/ns @last-ns}
            ;; (prn :pending @pending)
            (-> (nbb/eval-expr nil nil
                               {:wrap vector
                                ;; TODO this is a huge workaround
                                ;; we should instead re-organize the code in nbb.core
                                :parse-fn (let [realized? (atom false)]
                                            (fn [_]
                                              (if-not @realized?
                                                (do
                                                  (reset! realized? true)
                                                  the-val)
                                                :sci.core/eof)))})
                (.then (fn [v]
                         (let [[val ns]
                               [(first v) (sci/eval-form @nbb/sci-ctx '*ns*)]]
                           (reset! last-ns ns)
                           (.close rl)
                           (if socket
                             (.write socket (prn-str val))
                             (prn val))
                           (let [pendingf @pending-forms]
                             (if-not (str/blank? pendingf)
                               (do (reset! pending-forms "")
                                   (input-handler socket rl pendingf))
                               (input-loop socket))))))
                (.catch (fn [err]
                          (prn (str err)))))))))))

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
