(ns example
  (:require [reagent.core :as r]
            ["react-dom/server" :as dom-server]
            ["mjml$default" :as mjml2html]
            ["node:fs" :as fs]))

(def example-email
  [:mjml
   [:mj-body
    [:mj-section {"background-color" "#F0F0F0"}
     [:mj-column
      [:mj-text {"font-style" "italic", "font-size" "20px", "color" "#626262"} "My Company"]]]
    [:mj-section {"background-url" "http://1.bp.blogspot.com/-TPrfhxbYpDY/Uh3Refzk02I/AAAAAAAALw8/5sUJ0UUGYuw/s1600/New+York+in+The+1960's+-+70's+(2).jpg",
                  "background-size" "cover",
                  "background-repeat" "no-repeat"}
     [:mj-column {"width" "600px"}
      [:mj-text {"align" "center", "color" "#FFF", "font-size" "40px", "font-family" "Helvetica Neue"} "Slogan here"]
      [:mj-button {"background-color" "#F63A4D" "href" "#"} "Promotion"]]]
    [:mj-section {"background-color" "#FAFAFA"}
     [:mj-column {"width" "400px"}
      [:mj-text {"font-style" "italic", "font-size" "20px", 
                 "font-family" "Helvetica Neue", "color" "#626262"} "My Awesome Text"]
      [:mj-text {"color" "#525252"} "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin rutrum enim eget magna efficitur, eu semper augue semper. Aliquam erat volutpat. Cras id dui lectus. Vestibulum sed finibus lectus, sit amet suscipit nibh. Proin nec commodo purus.
          Sed eget nulla elit. Nulla aliquet mollis faucibus."]
      [:mj-button {"background-color" "#F45E43" "href" "#"} "Learn more"]]]
    [:mj-section {"background-color" "#FFF"}
     [:mj-column 
      [:mj-image {"width" "200px", 
                  "src" "https://designspell.files.wordpress.com/2012/01/sciolino-paris-bw.jpg"}]]
     [:mj-column
      [:mj-text {"font-style" "italic", "font-size" "20px",
                 "font-family" "Helvetica Neue", "color" "#626262"} "Find amazing places"]
      [:mj-text {"color" "#525252"} "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin rutrum enim eget magna efficitur, eu semper augue semper. Aliquam erat volutpat. Cras id dui lectus. Vestibulum sed finibus lectus."]]]
    [:mj-section {"background-color" "#FBFBFB"}
     [:mj-column
      [:mj-image {"width" "100px", "src" "http://191n.mj.am/img/191n/3s/x0l.png"}]]
     [:mj-column
      [:mj-image {"width" "100px", "src" "http://191n.mj.am/img/191n/3s/x01.png"}]]
     [:mj-column
      [:mj-image {"width" "100px", "src" "http://191n.mj.am/img/191n/3s/x0s.png"}]]
     ]]])

(def output
  (mjml2html (.renderToStaticMarkup dom-server (r/as-element example-email))
  #js {}))

(fs/writeFileSync "output.html" (.-html output))

(comment
  (def my-email
    [:mjml
     [:mj-body
      [:mj-section
       [:mj-column
        [:mj-image {"width" "100px", "src" "https://mjml.io/assets/img/logo-small.png"}]
        [:mj-divider {"border-color" "#F45E43"}]
        [:mj-text {"font-size" "20px", "color" "#F45E43", "font-family" "Helvetica"} "Hello Clojurian!"]]]]])

  (def output
    (mjml2html (.renderToStaticMarkup dom-server (r/as-element example-email))
               #js {}))

  (js/console.log (.-html output))
  )
