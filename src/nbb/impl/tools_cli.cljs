(ns nbb.impl.tools-cli
  {:no-doc true}
  (:require [clojure.tools.cli :as tools.cli]
            [nbb.core :as nbb]
            [sci.core :as sci :refer [copy-var]]))

(def cli-ns (sci/create-ns 'clojure.tools.cli nil))

(def tools-cli-namespace
  {'format-lines (copy-var tools.cli/format-lines cli-ns)
   'summarize (copy-var tools.cli/summarize cli-ns)
   'get-default-options (copy-var tools.cli/get-default-options cli-ns)
   'parse-opts (copy-var tools.cli/parse-opts cli-ns)
   'make-summary-part (copy-var tools.cli/make-summary-part cli-ns)})

(defn init []
  (nbb/register-plugin!
   ::tools-cli
   {:namespaces {'clojure.tools.cli tools-cli-namespace}}))
