(ns nbb.promesa
  (:refer-clojure :exclude [delay spread promise
                            await map mapcat run!
                            future let loop recur])
  (:require #_[clojure.core :as c]
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
  `(pt/-bind nil (fn [_#]
                   ~(condp = (count exprs)
                      0 `(pt/-promise nil)
                      1 `(pt/-promise ~(first exprs))
                      (reduce (fn [acc e]
                                `(pt/-bind ~e (fn [_#] ~acc)))
                              `(pt/-promise ~(last exprs))
                              (reverse (butlast exprs)))))))

(defn ^:macro let
  "A `let` alternative that always returns promise and waits for all the
  promises on the bindings."
  [_ _ bindings & body]
  `(pt/-bind nil (fn [_#]
                   ~(->> (reverse (partition 2 bindings))
                         (reduce (fn [acc [l r]]
                                   `(pt/-bind ~r (fn [~l] ~acc)))
                                  `(promesa.core/do! ~@body))))))

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
   'done?    (sci/copy-var p/done? pns)
   'wrap    (sci/copy-var p/wrap pns)
   'then    (sci/copy-var p/then pns)
   'chain   (sci/copy-var p/chain pns)
   'catch   (sci/copy-var p/catch pns)
   'finally (sci/copy-var p/finally pns)
   'race    (sci/copy-var p/race pns)
   '->      (sci/copy-var p/-> pns)
   '->>     (sci/copy-var p/->> pns)
   'as->    (sci/copy-var p/as-> pns)})

(def promesa-protocols-namespace
  {'-bind (sci/copy-var pt/-bind ptns)
   '-promise (sci/copy-var pt/-promise ptns)})

(defn init []
  (nbb/register-plugin!
   ::promesa
   {:namespaces {'promesa.core promesa-namespace
                 'promesa.protocols promesa-protocols-namespace}}))
