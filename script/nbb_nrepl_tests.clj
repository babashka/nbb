(ns nbb-nrepl-tests
  (:require
   [babashka.process :refer [process]]
   [babashka.wait :refer [wait-for-port]]
   [bencode.core :as bencode]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]])
  (:import [java.net Socket]))


(def debug? false)

(def port (atom 13337))

(defn nrepl-server []
  (process ["node" "lib/nbb_main.js" "nrepl-server" ":port" @port]
           (merge {:out :inherit
                   :err :inherit})))

(defn bytes->str [x]
  (if (bytes? x) (String. (bytes x))
      (str x)))

(defn read-msg [msg]
  (let [res (zipmap (map keyword (keys msg))
                    (map #(if (bytes? %)
                            (String. (bytes %))
                            %)
                         (vals msg)))
        res (if-let [status (:status res)]
              (assoc res :status (mapv bytes->str status))
              res)
        res (if-let [status (:sessions res)]
              (assoc res :sessions (mapv bytes->str status))
              res)]
    res))

(defn read-reply [in session id]
  (loop []
    (let [msg (read-msg (bencode/read-bencode in))]
      (if (and (= (:session msg) session)
               (= (:id msg) id))
        (do
          (when debug? (prn "received" msg))
          msg)
        (do
          (when debug? (prn "skipping over msg" msg))
          (recur))))))

(deftest print-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "session"
        (is session))
      (testing "print"
        (bencode/write-bencode os {"op" "eval" "code" "(println :hello)"
                                   "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              out (:out msg)
              _ (is (= ":hello" out))
              msg (read-reply in session @id)
              out (:out msg)
              _ (is (= "\n" out))
              msg (read-reply in session @id)
              v (:value msg)
              _ (is (= "nil" v))
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))]))
      (testing "print with delay"
        (bencode/write-bencode os {"op" "eval" "code"
                                   "(require '[promesa.core :as p])
                                    (p/->> (p/delay 1000 {:delayed-by \"1 second\"}) prn)"
                                   "session" session "id" (new-id!)})
        (let [_msg (read-reply in session @id)
              _msg (read-reply in session @id)
              msg (read-reply in session @id)
              out (:out msg)
              _ (is (= "{:delayed-by \"1 second\"}" out))]))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest promise-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "print"
        (bencode/write-bencode os {"op" "eval" "code" "(js/Promise.resolve 1)"
                                   "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              v (:value msg)
              _ (is (str/includes? v "Promise"))
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))]))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest doc-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "doc"
        (bencode/write-bencode os
                               {"op" "eval" "code" "(doc ==)"
                                "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "-------------------------" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "\n" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "clojure.core/==" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "\n" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "([x] [x y] [x y & more])" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "\n" v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= (str "  Returns non-nil if nums all have the equivalent"
                            "\n  value, otherwise false. Behavior on non nums is"
                            "\n  undefined.") v))
              msg (read-reply in session @id)
              v (:out msg)
              _ (is (= "\n" v))
              msg (read-reply in session @id)
              v (:value msg)
              _ (is (= "nil" v))
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))]))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest complete-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "complete"
        (bencode/write-bencode os {"op" "eval" "code" "(require '[nbb.core :as nbb] '[\"fs\" :as fs])"
                                   "session" session "id" (new-id!)})
        (let [_ (read-reply in session @id)
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))])
        (testing "SCI var completions"
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "nbb/"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "nbb/load-string", :ns "nbb.core"}))
            (is (contains? completions {:candidate "nbb/await",       :ns "nbb.core"}))))
        (testing "JS import completions"
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "fs/"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "fs/readlink"}))))
        (testing "JS import completions with property access"
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "fs/constants."
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "fs/constants.COPYFILE_EXCL"})))
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "fs/constants.C"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "fs/constants.COPYFILE_EXCL"})))))
      (testing "js global"
        (bencode/write-bencode os
                               {"op" "complete" "symbol" "js/"
                                "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              completions (:completions msg)
              completions (set (map read-msg completions))]
          (is (contains? completions {:candidate "js/console"})))
        (testing "property access"
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "js/console."
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "js/console.log"})))
          (bencode/write-bencode os
                                 {"op" "complete" "symbol" "js/console.l"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "js/console.log"})))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest pprint-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (doseq [pprint-fn ["clojure.pprint/pprint"
                         "cider.nrepl.pprint/pprint"]]
        (testing "print"
          (bencode/write-bencode os {"op" "eval"
                                     "code" "(range 20)"
                                     "nrepl.middleware.print/print" pprint-fn
                                     "nrepl.middleware.print/options" {:length 10}
                                     "session" session "id" (new-id!)})

          (let [reply (read-reply in session @id)
                {:keys [value]} reply]
            (is (= "(0 1 2 3 4 5 6 7 8 9 ...)" (str/trim value))))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest eldoc-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "eldoc"
        (bencode/write-bencode os {"op" "eval"
                                   "code"
                                   "(ns example)"
                                   "session" session "id" (new-id!)})
        (let [_ (read-reply in session @id)
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))])
        (testing "Core eldoc"
          (bencode/write-bencode os
                                 {"op" "eldoc"
                                  "ns" "example"
                                  "symbol" "prn"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                ;; fixme:
                ;; (println (first (first (:eldoc msg))))
                ;; #object[[B 0x4b7c736d [B@4b7c736
                ;; I don't get how to get the strings
                ;; cider correctly makes this out of :eldoc
                ;; '(("&" "objs"))
                _ (is (= (:name msg) "prn"))
                _ (is (= (:ns msg) "clojure.core"))
                _ (is (= (:type msg) "function"))])))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))
(deftest lookup-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "eldoc"
        (bencode/write-bencode os {"op" "eval"
                                   "code"
                                   "(ns example)"
                                   "session" session "id" (new-id!)})
        (let [_ (read-reply in session @id)
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))])
        (testing "Core lookup"
          (bencode/write-bencode os
                                 {"op" "lookup"
                                  "ns" "example"
                                  "symbol" "prn"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                _ (is (= (:arglists-str msg) "[& objs]"))
                _ (is (= (:name msg) "prn"))
                _ (is (= (:ns msg) "clojure.core"))]))
        (testing "Core info"
          (bencode/write-bencode os
                                 {"op" "info"
                                  "ns" "example"
                                  "symbol" "prn"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                _ (is (= (:arglists-str msg) "[& objs]"))
                _ (is (= (:name msg) "prn"))
                _ (is (= (:ns msg) "clojure.core"))])))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))
(deftest macroexpand-test
  (swap! port inc)
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          new-id! #(swap! id inc)]
      (testing "eldoc"
        (bencode/write-bencode os {"op" "eval"
                                   "code"
                                   "(ns example)\n(defmacro foo [] 'lul)"
                                   "session" session "id" (new-id!)})
        (let [_ (read-reply in session @id)
              msg (read-reply in session @id)
              status (:status msg)
              _ (is (= ["done"] status))])
        (testing "Expand user macro"
          (bencode/write-bencode os
                                 {"op" "macroexpand"
                                  "code" "(foo)"
                                  "expander" "macroexpand-1"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                _ (is (= (:expansion msg) "lul"))]))
        (testing "Expand core macro"
          (bencode/write-bencode os
                                 {"op" "macroexpand"
                                  "code" "(when 'foo 'bar)"
                                  "expander" "macroexpand-1"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                _ (is (= (:expansion msg) "(if (quote foo) (do (quote bar)))"))])))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))


(defn -main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-nrepl-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
