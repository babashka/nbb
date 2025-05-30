(ns nbb.impl.repl
  (:require
   ["node:net" :as net]
   ["node:readline" :as readline]
   ["node:vm" :as vm]
   ["node:process" :as process]
   [clojure.string :as str]
   [nbb.api :as api]
   [nbb.core :as nbb]
   [nbb.impl.repl-utils :refer [handle-complete*]]
   [sci.core :as sci])
  (:require-macros [nbb.macros :as macros]))

(def last-ns (atom @sci/ns))

(defn completer*
  "Given a line, returns a flat vector of completions"
  [line]
  (let [[prefix line] (if-let [idx (str/last-index-of line "(")]
                        [(subs line 0 (inc idx)) (subs line (inc idx))]
                        [nil line])
        xs (get (try (handle-complete* {:ns (str @last-ns)
                                        :prefix line})
                     (catch :default e
                       (js/console.warn (str :warn) (ex-message e))
                       nil))
                "completions")
        xs (map #(str prefix %)
                (filterv #(str/starts-with? % line)
                         (map #(get % "candidate") xs)))]
    xs))

(defn completer [line]
  #js [(into-array (completer* line)) line])

(def pending-input (atom ""))

(declare input-loop eval-next)

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

(def tty (and (-> process/stdout.isTTY)
              (-> process/stdin.setRawMode)))

(defn eval-expr [socket f]
  (let [ctx #js {:f f}
        _ (.createContext vm ctx)]
    (try
      (when (and tty (not socket))
        (.setRawMode (-> process/stdin) false))
      (-> (.runInContext vm "f()" ctx
                         #js {:displayErrors true
                              ;; :timeout 1000
                              :breakOnSigint true
                              :microtaskMode "afterEvaluate"})
          (.then (fn [wrapper]
                   (let [ctx #js {:f (if socket (fn [] wrapper)
                                         (fn []
                                           (let [v (first wrapper)]
                                             (prn v)
                                             wrapper)))}
                         _ (.createContext vm ctx)]
                     (.runInContext vm "f()" ctx
                                    #js {:displayErrors true
                                         ;; :timeout 1000
                                         :breakOnSigint true
                                         :microtaskMode "afterEvaluate"}))))
          (.finally (fn []
                      (when (and tty (not socket))
                        (.setRawMode process/stdin true)))))
      (catch :default e
        (when (and tty (not socket))
          (.setRawMode process/stdin true))
        (js/Promise.reject e)))))

(defn eval-next [socket rl]
  (when-not (or @in-progress (str/blank? @pending-input))
    (reset! in-progress true)
    (let [rdr (sci/reader @pending-input)
          the-val (try (sci/binding [sci/ns @last-ns]
                         (nbb/parse-next rdr))
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
                    (-> (eval-expr
                         socket
                         #(nbb/eval-next* the-val
                                          {:ns @last-ns
                                           :file @sci/file
                                           :wrap vector}))
                        (.then (fn [v]
                                 (let [[val {:keys [ns]}] v]
                                   (reset! last-ns ns)
                                   (sci/alter-var-root sci/*3 (constantly @sci/*2))
                                   (sci/alter-var-root sci/*2 (constantly @sci/*1))
                                   (sci/alter-var-root sci/*1 (constantly val))
                                   (when socket
                                     (.write socket (prn-str val))
                                     #_(prn val))
                                   (continue rl socket))))
                        (.catch (fn [err]
                                  (prn (str err))
                                  (sci/alter-var-root sci/*e (constantly err))
                                  (continue rl socket)))))
                  (reset! in-progress false)))))))

(defn input-handler [socket rl input]
  (swap! pending-input str input "\n")
  (eval-next socket rl))

(defn on-line [^js rl socket]
  (.on rl "line" #(input-handler socket rl %)))

(defn create-rl []
  (.createInterface
   readline #js {:input (-> process/stdin)
                 :output (-> process/stdout)
                 :completer completer}))

(defn create-socket-rl [socket]
  (.createInterface
   readline #js {:input socket
                 :output socket
                 :completer completer}))

(defn input-loop [socket resolve]
  (let [rl (if socket
             (create-socket-rl socket)
             (create-rl))
        _ (on-line rl socket)
        _ (.setPrompt rl (str @last-ns "=> "))
        _ (.on rl "close" resolve)]
    (.prompt rl)))

(defn on-connect [socket]
  (let [rl (create-socket-rl socket)]
    (on-line rl socket))
  (.setNoDelay ^net/Socket socket true)
  (.on ^net/Socket socket "close"
       (fn [_had-error?]
         (println "Client closed connection."))))

(def rns (sci/create-ns 'nbb.repl nil))

(defn socket-repl
  ([] (socket-repl nil))
  ([opts]
   (let [port (or (:port opts)
                  0)
         srv (net/createServer
              on-connect)]
     (.listen srv port "127.0.0.1"
              (fn []
                (let [addr (-> srv (.address))
                      port (-> addr .-port)
                      host (-> addr .-address)]
                  (println (str "Socket REPL listening on port "
                                port " on host " host))))))))

(defn repl
  ([] (repl nil))
  ([opts]
   (when tty (.setRawMode process/stdin true))
   (let [eval-require (fn
                        [ns-form]
                        (nbb/eval-require
                         (list
                          'quote
                          (list 'quote ns-form))))
         [ns1 ns2] nbb/repl-requires]
     (println (str "Welcome to " (nbb/cli-name) " v" (api/version) "!"))
     (->
      (eval-require ns1)
      (.then (fn [] (eval-require ns2)))
      (.then (:init opts identity))
      (.then (fn []
               (js/Promise. (fn [resolve]
                              (input-loop nil resolve)))))))))

(def repl-namespace
  {'repl (sci/copy-var repl rns)
   'get-completions (sci/copy-var completer* rns)
   'socket-repl (sci/copy-var socket-repl rns)})

(defn init []
  (nbb/register-plugin!
   :nbb.repl
   {:namespaces {'nbb.repl repl-namespace}}))
