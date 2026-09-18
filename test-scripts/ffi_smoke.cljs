(ns ffi-smoke
  (:require [babashka.ffi :as ffi]))

(ffi/defcfn strlen "strlen" [:string] :size_t)

(def abs* (ffi/cfn "abs" [:int] :int))

(def result
  {:strlen (strlen "hello")
   :abs (abs* -5)
   :backend (:babashka.ffi/backend (meta abs*))
   :read (ffi/with-open [arena (ffi/confined-arena)]
           (let [p (ffi/alloc arena :int)]
             (ffi/write p :int 42)
             (ffi/read p :int)))})

(def expected {:strlen 5 :abs 5 :backend :node :read 42})

(if (= expected result)
  (println "ffi smoke test passed:" result)
  (do (println "ffi smoke test failed")
      (println "expected:" expected)
      (println "got:     " result)
      (set! (.-exitCode js/process) 1)))
