;; This is a demonstration of pdfkit a library for creating PDFs
;; Also uses svg-to-pdfkit for loading SVG
;; Produces 'output.pdf' on run
(ns example
  (:require ["fs" :as fs]
            ["pdfkit$default" :as PDFDocument]
            ["svg-to-pdfkit$default" :as SVGtoPDF]))

(def lorem (fs/readFileSync "lorem.txt" "utf-8"))

(def logo (fs/readFileSync "../../logo/nbb-with-name.svg" "utf-8"))

;; start building a new document
(def doc (PDFDocument.))

;; pipe result to a file
(.pipe doc (.createWriteStream fs "output.pdf"))

;; add nbb logo
(SVGtoPDF doc logo 200 200 #js {})

(-> doc
    (.addPage)
    (.font "Times-Roman" 18)
    (.text "Nbb is awesome!"))

;; draw a triangle
(-> doc
    (.save)
    (.moveTo 100 150)
    (.lineTo 100 250)
    (.lineTo 200 250)
    (.fill "#FF3300"))

;; draw a circle
(-> doc
    (.circle 280 200 50)
    (.fill "#6600FF"))

;; draw a star like object
(-> doc
    (.scale 0.6)
    (.translate 470 130)
    (.path "M 250,75 L 323,301 131,161 369,161 177,301 z")
    (.fill "red" "even-odd")
    (.restore))

;; write down text into a two columns with justify alignment
(-> doc
    (.text "And here is some wrapped text..." 100 300)
    (.font "Times-Roman" 13)
    (.moveDown)
    (.text lorem
           #js {"width" 412
                "align" "justify"
                "indent" 30
                "columns" 2
                "height" 300
                "elipsis" true}))

;; flush the stream
(.end doc)
