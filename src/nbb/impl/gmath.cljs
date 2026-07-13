(ns nbb.impl.gmath
  (:require [goog.math.Long]
            [nbb.core :as nbb]
            [sci.core :as sci]))

(def gns (sci/create-ns 'goog.math.Long nil))

(def goog-math-long-namespace
  {'getMaxValue goog.math.Long/getMaxValue
   'getMinValue goog.math.Long/getMinValue
   'getZero goog.math.Long/getZero
   'getOne goog.math.Long/getOne
   'fromInt goog.math.Long/fromInt
   'fromNumber goog.math.Long/fromNumber
   'fromString goog.math.Long/fromString})

(defn init []
  (nbb/register-plugin!
   ::goog_math_long
   {:classes {'goog.math.Long goog.math.Long}
    :namespaces {'goog.math.Long goog-math-long-namespace}}))
