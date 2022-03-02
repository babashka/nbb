(ns example
  (:require ["asciidoctor$default" :as asciidoctor]))

(def ascii (asciidoctor))

(def content "http://asciidoctor.org[*Asciidoctor*]
running on https://opalrb.com[_Opal_]
brings AsciiDoc to Node.js!")

(def html (.convert ascii content))

(println html)
;; <div class="paragraph">
;; <p><a href="http://asciidoctor.org"><strong>Asciidoctor</strong></a>
;; running on <a href="https://opalrb.com"><em>Opal</em></a>
;; brings AsciiDoc to Node.js!</p>
;; </div>
