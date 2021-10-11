(ns example
  "From https://stackoverflow.com/a/61278578/6264"
  (:require ["pdfjs-dist/legacy/build/pdf$default" :as pdfjs]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn get-text [path]
  (p/let [doc (.-promise (.getDocument pdfjs path))
          page1 (.getPage doc 1)
          content (.getTextContent page1)
          strings (map #(.-str %) (.-items content))]
    (str/join "\n" strings)))

(p/let [strs (get-text "pdf-test.pdf")]
  (println strs))


