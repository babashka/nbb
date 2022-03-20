(ns example
  (:require ["sharp$default" :as sharp]
            [nbb.core :refer [await]]))

;; see https://sharp.pixelplumbing.com/ for docs

(await (-> (sharp "./clojure_logo.svg")
           (.toFile "./clojure_logo.png")))
;;=> #js {:format "png", :width 256, :height 256, :channels 4, :premultiplied false, :size 13165}

