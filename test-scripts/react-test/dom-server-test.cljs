(require '[reagent.dom.server :as srv])
(prn (srv/render-to-static-markup [:div [:a 1]]))
