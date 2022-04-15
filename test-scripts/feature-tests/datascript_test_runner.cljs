(ns test-runner
  (:require [cljs.test :as t]
            [nbb.core :as nbb]
            [datascript.test.query]
            [datascript.test.pull-api]
            [datascript.test.query-rules]
            [datascript.test.conn]
            [datascript.test.explode]
            [datascript.test.filter]
            [datascript.test.ident]
            [datascript.test.query-fns]
            [datascript.test.query-not]
            [datascript.test.query-or]
            [datascript.test.query-aggregates]
            [datascript.test.query-pull]
            [datascript.test.query-return-map]
            [datascript.test.tuples]
            [datascript.test.query-find-specs]
            [datascript.test.transact]))

(defn init []
  (t/run-tests 'datascript.test.query
               'datascript.test.pull-api
               'datascript.test.query-rules
               'datascript.test.conn
               'datascript.test.explode
               'datascript.test.filter
               'datascript.test.ident
               'datascript.test.query-fns
               'datascript.test.tuples
               'datascript.test.transact
               'datascript.test.query-pull
               'datascript.test.query-find-specs
               'datascript.test.query-return-map
               'datascript.test.query-not
               'datascript.test.query-aggregates
               'datascript.test.query-or))

(when (= nbb/*file* (:file (meta #'init)))
  (init))
