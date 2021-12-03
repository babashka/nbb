(ns example
  (:require
   ["fs" :as fs]
   ["web-audio-api$default" :as wa]))

(def ^:dynamic context (wa/AudioContext.))

(set! (.-outStream context) (.-stdout js/process))

(fs/readFile "outfoxing.mp3"
      (fn [err buffer]
        (when err (throw err))
        (.decodeAudioData
          context
          buffer
          (fn [audioBuffer]
            (let [bufferNode (.createBufferSource context)]
              (.connect bufferNode (.-destination context))
              (set! (.-buffer bufferNode) audioBuffer)
              (set! (.-loop bufferNode) true)
              (.start bufferNode 0))))))
