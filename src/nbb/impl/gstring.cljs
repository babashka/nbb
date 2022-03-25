(ns nbb.impl.gstring
  (:require [goog.string :as gstr]
            [goog.string.format]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def gns (sci/create-ns 'cljs-bean.core nil))

(def goog-string-namespace
  {'format gstr/format})

(defn init []
  (nbb/register-plugin!
   ::goog_string
   {:classes {'goog.string #js {:format gstr/format}}
    :namespaces {'goog.string goog-string-namespace}}))
