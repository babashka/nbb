(ns nbb-nrepl-tests
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [process]]
   [babashka.wait :refer [wait-for-port]]
   [bencode.core :as bencode]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]
   [test-utils])
  (:import [java.net Socket]))

(def debug? false)

(def port (atom 13337))

(defn nrepl-server []
  (swap! port inc)
  (process ["node" "lib/nbb_main.js" "nrepl-server" ":port" @port]
           {:out :inherit
            :err :inherit}))

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

(defn- read-until-done [in ids]
  (loop [replies {} pending (set ids)]
    (if (empty? pending)
      replies
      (let [{:keys [id status] :as msg} (read-msg (bencode/read-bencode in))]
        (recur (update replies id (fnil conj []) msg)
               (cond-> pending (some #{"done"} status) (disj id)))))))

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
              ns (:ns msg)
              _ (is (= "user" ns))
              status (:status msg)
              _ (is (= ["done"] status))]))
      (testing "do form"
        (bencode/write-bencode os {"op" "eval" "code" "(do (println :in-do) :foo)"
                                   "session" session "id" (new-id!)})
        (let [_out (read-reply in session @id)
              _nl (read-reply in session @id)
              msg (read-reply in session @id)
              _ (is (= ":foo" (:value msg)))
              msg (read-reply in session @id)
              _ (is (= ["done"] (:status msg)))]))
      (testing "print with delay"
        (bencode/write-bencode os {"op" "eval" "code"
                                   "(require '[promesa.core :as p])
                                    (p/->> (p/delay 1000 {:delayed-by \"1 second\"}) prn)"
                                   "session" session "id" (new-id!)})
        (let [_require-val (read-reply in session @id)
              out-msg (read-reply in session @id)
              out (:out out-msg)
              _ (is (= "{:delayed-by \"1 second\"}" out))]))
      (testing "async println"
        (bencode/write-bencode os {"op" "eval" "code"
                                   "(js/setTimeout #(do (println 123)) 10)"
                                   "session" session "id" (new-id!)})
        (let [_tm (read-reply in session @id)
              done (read-reply in session @id)
              _ (is (= ["done"] (:status done)))
              msg (read-reply in session @id)
              _ (is (= "123" (:out msg)))
              nl (read-reply in session @id)
              _ (is (= "\n" (:out nl)))]))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest promise-test
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
  (doseq [op ["complete" "completions"]]
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
          (bencode/write-bencode os {"op" "eval" "code" "(def *** 1)"
                                     "session" session "id" (new-id!)})
          (let [_ (read-reply in session @id)
                msg (read-reply in session @id)
                status (:status msg)
                _ (is (= ["done"] status))])
          (testing "SCI var completions"
            (bencode/write-bencode os
                                   {"op" op "symbol" "nbb/"
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "nbb/load-string", :ns "nbb.core"}))
              (is (contains? completions {:candidate "nbb/await",       :ns "nbb.core"}))))
          (testing "special characters"
            (bencode/write-bencode os
                                   {"op" op "symbol" "*"
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "***", :ns "user"}))
              (is (contains? completions {:candidate "*print-readably*", :ns "clojure.core"}))))
          (testing "JS import completions"
            (bencode/write-bencode os
                                   {"op" op "symbol" "fs/"
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "fs/readlink"}))))
          (testing "JS import completions with property access"
            (bencode/write-bencode os
                                   {"op" op "symbol" "fs/constants."
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "fs/constants.COPYFILE_EXCL"})))
            (bencode/write-bencode os
                                   {"op" op "symbol" "fs/constants.C"
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "fs/constants.COPYFILE_EXCL"})))))
        (testing "js global"
          (bencode/write-bencode os
                                 {"op" op "symbol" "js/"
                                  "session" session "id" (new-id!)})
          (let [msg (read-reply in session @id)
                completions (:completions msg)
                completions (set (map read-msg completions))]
            (is (contains? completions {:candidate "js/console"})))
          (testing "property access"
            (bencode/write-bencode os
                                   {"op" op "symbol" "js/console."
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "js/console.log"})))
            (bencode/write-bencode os
                                   {"op" op "symbol" "js/console.l"
                                    "session" session "id" (new-id!)})
            (let [msg (read-reply in session @id)
                  completions (:completions msg)
                  completions (set (map read-msg completions))]
              (is (contains? completions {:candidate "js/console.log"})))))
        (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                   "session" session "id" (new-id!)})))))

(deftest pprint-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (let [id (atom 0)
          send! (fn [msg]
                  (let [id (swap! id inc)]
                    (bencode/write-bencode os (assoc msg "id" id))
                    id))
          clone! (fn []
                   (:new-session (first (get (read-until-done in [(send! {"op" "clone"})])
                                             @id))))
          values (fn [msg]
                   (let [id (send! (assoc msg "op" "eval"))]
                     (keep :value (get (read-until-done in [id]) id))))
          a (clone!)
          b (clone!)]
      (doseq [printer ["cider.nrepl.pprint/pprint"
                       "cider.nrepl.pprint/clojure-pprint"
                       "nrepl.util.print/pprint"]]
        (testing (str printer " prints with :length 10")
          (is (= ["(0 1 2 3 4 5 6 7 8 9 ...)"]
                 (values {"session" a "code" "(range 20)"
                          "nrepl.middleware.print/print" printer
                          "nrepl.middleware.print/options" {:length 10}})))))
      (testing "nrepl.util.print/pr prints with :print-length 3"
        (is (= ["(0 1 2 ...)"]
               (values {"session" a "code" "(range 10)"
                        "nrepl.middleware.print/print" "nrepl.util.print/pr"
                        "nrepl.middleware.print/options" {:print-length 3}}))))
      (values {"session" a "code" "(set! *print-length* 3)"})
      (testing "set! *print-length* applies to the next eval of the session"
        (is (= ["(0 1 2 ...)"] (values {"session" a "code" "(range 10)"}))))
      (testing "nrepl.util.print/pprint without :length uses the *print-length* of the session"
        (is (= ["(0 1 2 ...)"]
               (values {"session" a "code" "(range 10)"
                        "nrepl.middleware.print/print" "nrepl.util.print/pprint"}))))
      (testing "set! *print-length* in session a leaves session b unchanged"
        (is (= ["(0 1 2 3 4 5 6 7 8 9)"] (values {"session" b "code" "(range 10)"}))))
      (values {"session" a "code" "(set! *print-namespace-maps* true)"})
      (doseq [printer [nil "nrepl.util.print/pr" "cider.nrepl.pprint/pprint"]]
        (testing (str (or printer "the default printer") " uses the *print-namespace-maps* of the session")
          (is (= ["#:a{:b 1, :c 2}"]
                 (values (cond-> {"session" a "code" "{:a/b 1 :a/c 2}"}
                           printer (assoc "nrepl.middleware.print/print" printer)))))))
      (send! {"op" "eval" "code" "(js/process.exit 0)" "session" a}))))

(deftest eldoc-test
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
(deftest lookup-not-found-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)]
      (doseq [[op status] [["info" "no-info"]
                           ["lookup" "no-info"]
                           ["eldoc" "no-eldoc"]]]
        (testing (str op " on an unknown symbol replies " status)
          (bencode/write-bencode os {"op" op "ns" "user" "sym" "nope"
                                     "session" session "id" (swap! id inc)})
          (is (= {:status ["done" status]}
                 (dissoc (read-reply in session @id) :id :session)))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (swap! id inc)}))))

(deftest macroexpand-test
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
              _msg (read-reply in session @id)
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

(deftest eval-multiple-test
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
      (testing "send multiple values to be evaluated"
        (bencode/write-bencode os {"op" "eval"
                                   "code"
                                   "(+ 1 2 3) (ns dude) (defn foo [] 1) (foo)"
                                   "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              _ (is (= "6" (:value msg)))
              _msg (read-reply in session @id) ;; ns
              msg (read-reply in session @id)
              _ (is (= "#'dude/foo" (:value msg)))
              msg (read-reply in session @id)
              _ (is (= "1" (:value msg)))
              msg (read-reply in session @id)
              _ (is (= ["done"] (:status msg)))]))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest error-test
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
      (testing "send multiple values to be evaluated"
        (bencode/write-bencode os {"op" "eval"
                                   "code"
                                   "x (+ 1 2 3)"
                                   "session" session "id" (new-id!)})
        (let [msg (read-reply in session @id)
              _ (is (str/includes? (:err msg) "x"))
              msg (read-reply in session @id)
              _ (is (:ex msg))
              _ (is (= ["eval-error"] (:status msg)))
              msg (read-reply in session @id)
              _ (is (= "6" (:value msg)))
              msg (read-reply in session @id)
              _ (is (= ["done"] (:status msg)))]))
      (doseq [[code ex root-ex]
              [["(throw (ex-info \"boom\" {}))"
                "class cljs.core.ExceptionInfo" "class cljs.core.ExceptionInfo"]
               ["(throw (js/TypeError. \"boom\"))"
                "class TypeError" "class TypeError"]
               ["(throw (ex-info \"boom\" {} (js/RangeError. \"cause\")))"
                "class cljs.core.ExceptionInfo" "class RangeError"]
               ["(.foo nil)"
                "class TypeError" "class TypeError"]
               ["(throw \"boom\")"
                "class String" "class String"]
               ["undefined-symbol"
                "class cljs.core.ExceptionInfo" "class cljs.core.ExceptionInfo"]
               ["(+ 1"
                "class cljs.core.ExceptionInfo" "class cljs.core.ExceptionInfo"]
               ["(js/Promise.reject (js/TypeError. \"boom\"))"
                "class TypeError" "class TypeError"]
               ["(js/Promise.reject nil)"
                "class nil" "class nil"]]]
        (testing (str code " sends eval-error with the exception class")
          (bencode/write-bencode os {"op" "eval" "code" code
                                     "session" session "id" (new-id!)})
          (let [msgs (loop [msgs []]
                       (let [msg (read-reply in session @id)
                             msgs (conj msgs msg)]
                         (if (some #{"done"} (:status msg))
                           msgs
                           (recur msgs))))
                msg (first (filter :ex msgs))]
            (is (= ex (:ex msg)))
            (is (= root-ex (:root-ex msg)))
            (is (= ["eval-error"] (:status msg))))))
      (doseq [[code values] [["(throw (ex-info \"boom\" {})) (+ 1 2 3)" ["6"]]
                             ["(undefined-fn) :after" [":after"]]
                             ["(js/Promise.reject (js/Error. \"boom\")) :after" [":after"]]
                             ["1 ) 2" ["1"]]]]
        (testing (str code " sends one ex and the values " values)
          (bencode/write-bencode os {"op" "eval" "code" code
                                     "session" session "id" (new-id!)})
          (let [msgs (loop [msgs []]
                       (let [msg (read-reply in session @id)
                             msgs (conj msgs msg)]
                         (if (some #{"done"} (:status msg))
                           msgs
                           (recur msgs))))]
            (is (= 1 (count (filter :ex msgs))))
            (is (= values (keep :value msgs))))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (new-id!)}))))

(deftest namespace-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          replies! (fn [msg]
                     (let [id (swap! id inc)]
                       (bencode/write-bencode os (merge {"op" "eval" "session" session "id" id}
                                                        msg))
                       (loop [replies []]
                         (let [reply (read-reply in session id)
                               replies (conj replies reply)]
                           (if (some #{"done"} (:status reply))
                             replies
                             (recur replies))))))
          eval! (fn [msg]
                  (let [replies (replies! msg)]
                    {:values (filterv some? (map :value replies))
                     :ns (:ns (peek replies))}))
          current-ns "(str *ns*)"]
      (eval! {"code" "(ns foo)"})
      (testing ":ns selects the namespace after an ns form"
        (is (= {:values ["\"user\""] :ns "user"}
               (eval! {"code" current-ns "ns" "user"}))))
      (testing ":ns leaves the session namespace unchanged"
        (is (= {:values ["\"foo\""] :ns "foo"}
               (eval! {"code" current-ns}))))
      (testing "an ns form in an eval with :ns leaves the session namespace unchanged"
        (is (= "\"bar\"" (peek (:values (eval! {"code" "(ns bar) (str *ns*)" "ns" "user"})))))
        (is (= {:values ["\"foo\""] :ns "foo"}
               (eval! {"code" current-ns}))))
      (testing "in-ns changes the namespace of the next form"
        (is (= "\"baz\"" (peek (:values (eval! {"code" "(in-ns 'baz) (str *ns*)"}))))))
      (testing "in-ns changes the session namespace"
        (is (= {:values ["\"baz\""] :ns "baz"}
               (eval! {"code" current-ns}))))
      (testing "the form after undefined-symbol runs in the namespace of the form before it"
        (let [replies (replies! {"code" "(ns qux) undefined-symbol (str *ns*)"})]
          (is (= "\"qux\"" (last (keep :value replies))))
          (testing "the ex reply has the namespace qux"
            (is (= "qux" (:ns (first (filter :ex replies)))))))
        (is (= {:values ["\"qux\""] :ns "qux"}
               (eval! {"code" current-ns}))))
      (testing "an unknown :ns replies namespace-not-found"
        (let [[reply] (replies! {"code" "(ns unknown-target)" "ns" "unknown"})]
          (is (= "unknown" (:ns reply)))
          (is (= #{"error" "namespace-not-found" "done"} (set (:status reply)))))
        (is (= {:values ["\"qux\""] :ns "qux"}
               (eval! {"code" current-ns}))))
      (testing "load-file ignores :ns and changes the session namespace"
        (is (= "\"loaded\"" (peek (:values (eval! {"op" "load-file"
                                                    "file" "(ns loaded) (str *ns*)"
                                                    "ns" "user"})))))
        (is (= {:values ["\"loaded\""] :ns "loaded"}
               (eval! {"code" current-ns}))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (swap! id inc)}))))

(deftest load-file-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (bencode/write-bencode os {"op" "clone"})
    (let [session (:new-session (read-msg (bencode/read-bencode in)))
          id (atom 0)
          request! (fn [msg]
                     (let [id (swap! id inc)]
                       (bencode/write-bencode os (assoc msg "session" session "id" id))
                       (loop [replies []]
                         (let [reply (read-reply in session id)
                               replies (conj replies reply)]
                           (if (some #{"done"} (:status reply))
                             replies
                             (recur replies))))))
          info (fn [sym]
                 (first (request! {"op" "info" "ns" "user" "sym" sym})))]
      (testing "load-file sets the :file of a var to file-path"
        (request! {"op" "load-file"
                   "file" "(ns load-file-a)\n(defn f [x] x)\n"
                   "file-path" "/tmp/load_file_a.cljs"
                   "file-name" "load_file_a.cljs"})
        (is (= {:file "/tmp/load_file_a.cljs" :line 2}
               (select-keys (info "load-file-a/f") [:file :line]))))
      (testing "load-file without file-path sets the :file of a var to file-name"
        (request! {"op" "load-file"
                   "file" "(ns load-file-b)\n(defn g [x] x)\n"
                   "file-name" "load_file_b.cljs"})
        (is (= "load_file_b.cljs" (:file (info "load-file-b/g")))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                 "session" session "id" (swap! id inc)}))))

(deftest session-test
  (nrepl-server)
  (wait-for-port "localhost" @port)
  (with-open [socket (Socket. "127.0.0.1" @port)
              in (.getInputStream socket)
              in (java.io.PushbackInputStream. in)
              os (.getOutputStream socket)]
    (let [id (atom 0)
          send! (fn [msg]
                  (let [id (swap! id inc)]
                    (bencode/write-bencode os (assoc msg "id" id))
                    id))
          clone! (fn [msg]
                   (:new-session (first (get (read-until-done in [(send! (assoc msg "op" "clone"))])
                                             @id))))
          values (fn [session code]
                   (let [id (send! {"op" "eval" "session" session "code" code})]
                     (keep :value (get (read-until-done in [id]) id))))
          a (clone! {})
          b (clone! {})]
      (values a "(ns foo) 42")
      (values a "(throw (ex-info \"boom\" {}))")
      (testing "an eval in session a leaves the namespace, *1 and *e of session b unchanged"
        (is (= ["[\"user\" nil false]"] (values b "[(str *ns*) *1 (some? *e)]"))))
      (testing "session a keeps its namespace, *1 and *e"
        (is (= ["[\"foo\" 42 true]"] (values a "[(str *ns*) *1 (some? *e)]"))))
      (testing "*1 and *2 change with each form of an eval"
        (is (= ["1" "2" "[2 1]"] (values b "1 2 [*1 *2]"))))
      (testing "clone with session copies the namespace and *1 of that session"
        (let [c (clone! {"session" a})]
          (is (= ["[\"foo\" true]"] (values c "[(str *ns*) (vector? *1)]")))))
      (testing "overlapping async evals keep their own bindings"
        (values a "(ns aaa)")
        (values b "(ns bbb)")
        (let [later (fn [ms] (str "(js/Promise. (fn [resolve] (js/setTimeout #(resolve (str *ns*)) " ms ")))"))
              id-a (send! {"op" "eval" "session" a "code" (later 100)})
              id-b (send! {"op" "eval" "session" b "code" (later 800)})
              replies (read-until-done in [id-a id-b])]
          (is (= ["#<Promise \"aaa\">"] (keep :value (get replies id-a))))
          (is (= ["#<Promise \"bbb\">"] (keep :value (get replies id-b))))))
      (testing "close removes the session"
        (let [c (clone! {"session" a})]
          (read-until-done in [(send! {"op" "close" "session" c})])
          (is (= ["\"user\""] (values c "(str *ns*)")))))
      (send! {"op" "eval" "code" "(js/process.exit 0)" "session" a}))))

(deftest disconnect-test
  (let [server (nrepl-server)]
    (wait-for-port "localhost" @port)
    (with-open [socket (Socket. "127.0.0.1" @port)]
      (.setSoLinger socket true 0)
      (bencode/write-bencode (.getOutputStream socket)
                             {"op" "eval" "id" 1
                              "code" "(js/Promise. (fn [resolve] (js/setTimeout #(resolve (println :late)) 200)))"}))
    (Thread/sleep 500)
    (testing "a connection reset during an eval leaves the server running"
      (is (.isAlive ^Process (:proc server))))
    (with-open [socket (Socket. "127.0.0.1" @port)
                in (java.io.PushbackInputStream. (.getInputStream socket))
                os (.getOutputStream socket)]
      (bencode/write-bencode os {"op" "eval" "code" "(+ 1 2)" "id" 2})
      (testing "a new connection evaluates (+ 1 2) after the reset"
        (is (= ["3"] (keep :value (get (read-until-done in [2]) 2)))))
      (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)" "id" 3}))))

(deftest print-outside-eval-test
  (let [script (fs/file (fs/create-temp-file {:prefix "nbb-nrepl" :suffix ".cljs"}))
        out-file (fs/file (fs/create-temp-file {:prefix "nbb-nrepl-out" :suffix ".txt"}))
        port (swap! port inc)
        ticks #(count (re-seq #":tick" (slurp out-file)))]
    (spit script (str "(require '[nbb.nrepl-server :as nrepl])
                       (js/setInterval #(println :tick) 50)
                       (nrepl/start-server! {:port " port "})"))
    (process ["node" "lib/nbb_main.js" (str script)]
             {:out :write :out-file out-file :err :inherit})
    (wait-for-port "localhost" port)
    (with-open [socket (Socket. "127.0.0.1" port)
                in (.getInputStream socket)
                in (java.io.PushbackInputStream. in)
                os (.getOutputStream socket)]
      (bencode/write-bencode os {"op" "clone"})
      (let [session (:new-session (read-msg (bencode/read-bencode in)))]
        (bencode/write-bencode os {"op" "eval" "code" "(println :hello)"
                                   "session" session "id" 1})
        (testing "output of an eval goes to the client"
          (is (= ":hello" (:out (read-reply in session 1)))))
        (loop []
          (when-not (= ["done"] (:status (read-reply in session 1)))
            (recur)))
        (testing "code that does not run in an eval keeps printing to stdout"
          (let [before (ticks)]
            (Thread/sleep 500)
            (is (> (ticks) before))))
        (testing "output of an eval is not printed to stdout"
          (is (not (str/includes? (slurp out-file) ":hello"))))
        (bencode/write-bencode os {"op" "eval" "code" "(js/process.exit 0)"
                                   "session" session "id" 2})))))

(defn -main [& _]
  (let [{:keys [:error :fail]} (t/run-tests 'nbb-nrepl-tests)]
    (when (pos? (+ error fail))
      (throw (ex-info "Tests failed" {:babashka/exit 1})))))
