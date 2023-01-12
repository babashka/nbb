(ns meander-test)

(require '[meander.epsilon :as m])

(defn favorite-food-info [foods-by-name user]
  (m/match {:user user
            :foods-by-name foods-by-name}
           {:user
            {:name ?name
             :favorite-food {:name ?food}}
            :foods-by-name {?food {:popularity ?popularity
                                   :calories ?calories}}}
           {:name ?name
            :favorite {:food ?food
                       :popularity ?popularity
                       :calories ?calories}}))

(def foods-by-name
  {:nachos {:popularity :high
            :calories :lots}
   :smoothie {:popularity :high
              :calories :less}})

(prn
 (favorite-food-info foods-by-name
                     {:name :alice
                      :favorite-food {:name :nachos}}))
