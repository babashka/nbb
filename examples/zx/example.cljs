(ns example
  (:require ["zx" :refer [$ chalk]]
            [promesa.core :as p]))

(-> (p/do ($ #js ["which rg"])
          (js/console.log (.green chalk "You already have rg, awesome")))
    (p/catch
        (fn [_]
          (js/console.log (.red chalk "Nope, installing rg (rigrep)"))
          ($ #js ["brew install ripgrep"]))))
