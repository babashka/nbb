(ns nbb-nrepl-tests
  (:require
   [babashka.process :refer [process]]
   [babashka.wait :refer [wait-for-port]]
   [bencode.core :as bencode]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.string :as str])
  (:import [java.net Socket]))


(def debug? false)

(def port (atom 13337))

(defn nrepl-server []
  (process ["node" "out/nbb_main.js" "nrepl-server" ":port" @port]
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

(defn -main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-nrepl-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
