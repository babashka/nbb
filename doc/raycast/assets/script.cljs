(ns script
  (:require ["@raycast/api" :as rc :refer [Form]]
            [reagent.core :as r]))

(defn Command []
  (r/as-element
   [:> rc/Form
    [:> Form.TextArea {:id "secret"
                       :title "secret"
                       :placeholder "secret"}]
    [:> Form.Dropdown {:id "expireViews"
                               :title "expire after views"
                               :storeValue true}
     [:> Form.Dropdown.Item {:value "1"
                             :title "1 view"}]]]))

#js {:Command Command}
