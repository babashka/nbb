(prn :foo)

(-> (js/import "fs")
    (.then (fn [fs]
             (println (str (.readFileSync fs "test.cljs"))))))
