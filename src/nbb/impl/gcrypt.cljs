(ns nbb.impl.gcrypt
  (:require [goog.crypt]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def gns (sci/create-ns 'goog.crypt nil))

(def goog-crypt-namespace
  {'byteArrayToHex goog.crypt/byteArrayToHex
   'byteArrayToString goog.crypt/byteArrayToString
   'hexToByteArray goog.crypt/hexToByteArray
   'stringToByteArray goog.crypt/stringToByteArray
   'stringToUtf8ByteArray goog.crypt/stringToUtf8ByteArray
   'utf8ByteArrayToString goog.crypt/utf8ByteArrayToString
   'xorByteArray goog.crypt/xorByteArray})

(defn init []
  (nbb/register-plugin!
   ::goog_crypt
   {:classes {'goog.crypt goog.crypt}
    :namespaces {'goog.crypt goog-crypt-namespace}}))
