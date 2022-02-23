(ns nbb.promesa
  (:refer-clojure :exclude [delay spread promise
                            await map mapcat run!
                            future let loop recur -> ->>
                            with-redefs])
  (:require [clojure.core :as c]
            [nbb.core :as nbb]
            [promesa.core :as p]
            [promesa.protocols :as pt]
            [sci.core :as sci]))

(def pns (sci/create-ns 'promesa.core nil))
(def ptns (sci/create-ns 'promesa.protocols nil))

(defn ^:macro do!
  "Execute potentially side effectful code and return a promise resolved
  to the last expression. Always awaiting the result of each
  expression."
  [_ _ & exprs]
  `(pt/-bind
    (pt/-promise nil)
    (fn [_#]
      ~(condp = (count exprs)
         0 `(pt/-promise nil)
         1 `(pt/-promise ~(first exprs))
         (reduce (fn [acc e]
                   `(pt/-bind (pt/-promise ~e) (fn [_#] ~acc)))
                 `(pt/-promise ~(last exprs))
                 (reverse (butlast exprs)))))))

(defn ^:macro let
  "A `let` alternative that always returns promise and waits for all the
  promises on the bindings."
  [_ _ bindings & body]
  `(pt/-bind
    (pt/-promise nil)
    (fn [_#]
      ~(c/->> (reverse (partition 2 bindings))
              (reduce (fn [acc [l r]]
                        `(pt/-bind (pt/-promise ~r) (fn [~l] ~acc)))
                      `(promesa.core/do! ~@body))))))

(defn ^:macro ->
  "Like the clojure.core/->, but it will handle promises in values
  and make sure the next form gets the value realized instead of
  the promise. Example using to fetch data in the browser with CLJS:
  Example:
  (p/-> (js/fetch #js {...}) ; returns a promise
        .-body)
  The result of a thread is a promise that will resolve to the
  end of the thread chain."
  [_ _ x & forms]
  (c/let [fns (mapv (fn [arg]
                      (c/let [[f & args] (if (sequential? arg)
                                           arg
                                           (list arg))]
                        `(fn [p#] (~f p# ~@args)))) forms)]
    `(p/chain (p/promise ~x) ~@fns)))

(defn ^:macro ->>
  "Like the clojure.core/->>, but it will handle promises in values
  and make sure the next form gets the value realized instead of
  the promise. Example using to fetch data in the browser with CLJS:
  Example:
  (p/->> (js/fetch #js {...}) ; returns a promise
         .-body
         read-string
         (mapv inc)
  The result of a thread is a promise that will resolve to the
  end of the thread chain."
  [_ _ x & forms]
  (c/let [fns (mapv (fn [arg]
                      (c/let [[f & args] (if (sequential? arg)
                                           arg
                                           (list arg))]
                        `(fn [p#] (~f ~@args p#)))) forms)]
    `(p/chain (p/promise ~x) ~@fns)))

(defn ^:macro with-redefs
  "Like clojure.core/with-redefs, but it will handle promises in
  body and wait until they resolve or reject before restoring the
  bindings. Useful for mocking async APIs.
  Example:
  (defn async-func [] (p/delay 1000 :slow-original))
  (p/with-redefs [async-func (fn [] (p/resolved :fast-mock))]
    (async-func))
  The result is a promise that will resolve to the last body form and
  upon resolving restores the bindings to their original values."
  [_ _ bindings & body]
  (c/let [names (take-nth 2 bindings)
          vals (take-nth 2 (drop 1 bindings))
          orig-val-syms (c/map (comp gensym #(str % "-orig-val__") name) names)
          temp-val-syms (c/map (comp gensym #(str % "-temp-val__") name) names)
          binds (c/map vector names temp-val-syms)
          resets (reverse (c/map vector names orig-val-syms))
          bind-value (fn [[k v]] (println k v) (list 'set! k v))]
    `(c/let [~@(c/interleave orig-val-syms names)
             ~@(c/interleave temp-val-syms vals)]
       ~@(c/map bind-value binds)
       #_(p/-> (p/do! ~@body)
             (p/finally
               (fn []
                 ~@(c/map bind-value resets)))))))

(def promesa-namespace
  {'do! (sci/copy-var do! pns)
   'let (sci/copy-var let pns)
   'all (sci/copy-var p/all pns)
   'any (sci/copy-var p/any pns)
   'resolved (sci/copy-var p/resolved pns)
   'rejected (sci/copy-var p/rejected pns)
   'deferred (sci/copy-var p/deferred pns)
   'promise  (sci/copy-var p/promise pns)
   'promise? (sci/copy-var p/promise? pns)
   'thenable? (sci/copy-var p/thenable? pns)
   'resolved? (sci/copy-var p/resolved? pns)
   'rejected? (sci/copy-var p/rejected? pns)
   'pending? (sci/copy-var p/pending? pns)
   'delay (sci/copy-var p/delay pns)
   'done?    (sci/copy-var p/done? pns)
   'wrap    (sci/copy-var p/wrap pns)
   'then    (sci/copy-var p/then pns)
   'chain   (sci/copy-var p/chain pns)
   'catch   (sci/copy-var p/catch pns)
   'finally (sci/copy-var p/finally pns)
   'race    (sci/copy-var p/race pns)
   'run!    (sci/copy-var p/run! pns)
   '->      (sci/copy-var -> pns)
   '->>      (sci/copy-var ->> pns)
   'with-redefs (sci/copy-var with-redefs pns)})

(def promesa-protocols-namespace
  {'-bind (sci/copy-var pt/-bind ptns)
   '-promise (sci/copy-var pt/-promise ptns)})

(defn init []
  (nbb/register-plugin!
   ::promesa
   {:namespaces {'promesa.core promesa-namespace
                 'promesa.protocols promesa-protocols-namespace}}))
