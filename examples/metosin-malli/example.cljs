(ns example
  (:require [malli.core :as m]))

(prn
 (m/parse
  [:* [:catn
       [:prop string?]
       [:val [:altn
              [:s string?]
              [:b boolean?]]]]]
  ["-server" "foo" "-verbose" true "-user" "joe"]))

;;=> [{:prop "-server", :val [:s "foo"]} {:prop "-verbose", :val [:b true]} {:prop "-user", :val [:s "joe"]}]

