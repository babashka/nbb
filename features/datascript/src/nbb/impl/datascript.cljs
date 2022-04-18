(ns nbb.impl.datascript
  {:no-doc true}
  (:require [datascript.core :as d]
            [datascript.db :as db]
            [nbb.core :as nbb]
            [sci.core :as sci :refer [copy-var]]))

(def core-ns (sci/create-ns 'datascript.core nil))
(def db-ns   (sci/create-ns 'datascript.db nil))

(def core-namespace
  {'q (copy-var d/q core-ns)
   'empty-db (copy-var d/empty-db core-ns)
   'db-with (copy-var d/db-with core-ns)
   'filter (copy-var d/filter core-ns)
   'init-db (copy-var d/init-db core-ns)
   'datom (copy-var d/datom core-ns)
   'datoms (copy-var d/datoms core-ns)
   'pull (copy-var d/pull core-ns)
   'pull-many (copy-var d/pull-many core-ns)
   'entity (copy-var d/entity core-ns)
   'tx0 (copy-var d/tx0 core-ns)
   'db (copy-var d/db core-ns)
   'squuid (copy-var d/squuid core-ns)
   'with (copy-var d/with core-ns)
   'touch (copy-var d/touch core-ns)
   'index-range (copy-var d/index-range core-ns)
   'listen! (copy-var d/listen! core-ns)
   'conn-from-db (copy-var d/conn-from-db core-ns)
   'conn-from-datoms (copy-var d/conn-from-datoms core-ns)
   'transact! (copy-var d/transact! core-ns)
   'create-conn (copy-var d/create-conn core-ns)
   'reset-conn! (copy-var d/reset-conn! core-ns)
   'from-serializable (copy-var d/from-serializable core-ns)
   'serializable (copy-var d/serializable core-ns)})

(def db-namespace
  {'db-from-reader    (copy-var db/db-from-reader db-ns)
   'datom-from-reader (copy-var db/datom-from-reader db-ns)
   'datom-added       (copy-var db/datom-added db-ns)
   'datom-tx          (copy-var db/datom-tx db-ns)
   'datom             (copy-var db/datom db-ns)
   'DB                (copy-var db/DB db-ns)
   'Datom             (copy-var db/Datom db-ns)})

(defn init []
  (nbb/register-plugin!
   ::datascript
   {:namespaces {'datascript.core core-namespace
                 'datascript.db   db-namespace}}))
