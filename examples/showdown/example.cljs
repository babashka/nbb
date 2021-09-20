(ns example
  (:require ["fs" :as fs]
            ["showdown$default" :as showdown]))

(let [converter (showdown/Converter.)
      text (str (fs/readFileSync "README.md"))]
  (println (.makeHtml converter text)))

;; output: <h1 id="showdownexample">showdown example</h1>
