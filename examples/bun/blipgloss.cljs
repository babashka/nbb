(ns blipgloss
  (:require ["blipgloss" :refer [NewStyle]]))

(def style (-> (NewStyle)
               (.Bold true)
               (.Foreground "#FAFAFA")
               (.Background "#7D56F4")
               (.PaddingTop 2)
               (.PaddingLeft 4)
               (.Width 22)))

(js/console.log (style.Render "Hello bun"))
