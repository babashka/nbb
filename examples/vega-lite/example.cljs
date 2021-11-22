(ns example
  (:require
   ["vega-lite" :as vl]
   ["vega" :as v]))

(def spec
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json",
   :description "A simple bar chart with embedded data.",
   :data
   {:values
    [{:a "A", :b 28}
     {:a "B", :b 55}
     {:a "C", :b 43}
     {:a "D", :b 91}
     {:a "E", :b 81}
     {:a "F", :b 53}
     {:a "G", :b 19}
     {:a "H", :b 87}
     {:a "I", :b 52}]},
   :mark "bar",
   :encoding
   {:x {:field "a", :type "nominal", :axis {:labelAngle 0}},
    :y {:field "b", :type "quantitative"}}})

(def vega-spec (-> spec clj->js vl/compile .-spec v/parse))

(def view (v/View. vega-spec #js{:renderer "svg"}))

(-> view .toSVG (.then js/console.log))
