(ns example
  "Based on the example from https://www.geeksforgeeks.org/node-jimp-rotate."
  (:require ["jimp$default" :as jimp]
            [promesa.core :as p]))

(p/let [image (jimp/read "https://media.geeksforgeeks.org/wp-content/uploads/20190328185307/gfg28.png")]
  (-> image
      (.rotate 55)
      (.write "rotate1.png")))
