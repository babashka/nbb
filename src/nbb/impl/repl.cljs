(ns nbb.impl.repl
  (:require
   ["net" :as net]
   ["path" :as path]
   ["readline" :as readline]
   ["vm" :as vm]
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

(declare input-loop eval-next)

(def last-ns (atom @sci/ns))

(def in-progress (atom false))

(defn continue [rl socket]
  (reset! in-progress false)
  (.setPrompt ^js rl (str @last-ns "=> "))
  (.prompt rl)
  (when-not (str/blank? @pending-input)
    (eval-next socket rl)))

(defn erase-processed [rdr]
  (let [line (sci/get-line-number rdr)
        col (sci/get-column-number rdr)
        lines (str/split-lines @pending-input)
        [line & lines] (drop (dec line) lines)
        edited (when line (subs line col))]
    (reset! pending-input (str/join "\n" (cons edited lines)))))

(defn eval-expr [f]
  (let [ctx #js {:f f}
        _ (.createContext vm ctx)]
    (try
      (.setRawMode js/process.stdin false)
      (-> (.runInContext vm "f()" ctx
                         #js {:displayErrors true
                              ;; :timeout 1000
                              :breakOnSigint true
                              :microtaskMode "afterEvaluate"})
          (.then (fn [wrapper]
                   (let [ctx #js {:f (fn []
                                       (let [v (first wrapper)]
                                         (prn v)
                                         wrapper))}
                         _ (.createContext vm ctx)]
                     (.runInContext vm "f()" ctx
                                    #js {:displayErrors true
                                         ;; :timeout 1000
                                         :breakOnSigint true
                                         :microtaskMode "afterEvaluate"})))))
         (catch :default _e
           (prn "error")
           (js/Promise.resolve [10])))))

(defn eval-next [socket rl]
  (when-not (or @in-progress (str/blank? @pending-input))
    (reset! in-progress true)
    (let [rdr (sci/reader @pending-input)
          the-val (try (nbb/parse-next rdr)
                       (catch :default e
                         (if (str/includes? (ex-message e) "EOF while reading")
                           ::eof-while-reading
                           (do (erase-processed rdr)
                               (prn (str e))
                               ::continue))))]
      (cond (= ::continue the-val)
            (continue rl socket)
            (= ::eof-while-reading the-val)
            ;; more input expected
            (reset! in-progress false)
            :else
            (do (erase-processed rdr)
                (if-not (= :sci.core/eof the-val)
                  (macros/with-async-bindings {sci/ns @last-ns}
                    ;; (prn :pending @pending)
                    (-> (eval-expr #(nbb/eval-expr nil nil
                                                  {:wrap vector
                                                   ;; TODO this is a huge workaround
                                                   ;; we should instead re-organize the code in nbb.core
                                                   :parse-fn (let [realized? (atom false)]
                                                               (fn [_]
                                                                 (if-not @realized?
                                                                   (do
                                                                     (reset! realized? true)
                                                                     the-val)
                                                                   :sci.core/eof)))}))
                        (.then (fn [v]
                                 (.setRawMode js/process.stdin true)
                                 (let [[val ns]
                                       [(first v) (sci/eval-form @nbb/sci-ctx '*ns*)]]
                                   (reset! last-ns ns)
                                   (if socket
                                     (.write socket (prn-str val))
                                     #_(prn val))
                                   (continue rl socket))))
                        (.catch (fn [err]
                                  (prn (str err))
                                  (continue rl socket)))))
                  (reset! in-progress false)))))))

(defn input-handler [socket rl input]
  (swap! pending-input str input "\n")
  (eval-next socket rl))

(defn on-line [^js rl socket]
  (.on rl "line" #(input-handler socket rl %)))

(defn create-socket-rl [socket]
  (.createInterface
   readline #js {:input socket
                 :output socket}))

(defn ^:export input-loop [socket]
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
  (prn js/process.pid)
  ;; (.on js/process "SIGINT" (fn [] (prn :sigint)))
  (.setRawMode js/process.stdin true)
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
