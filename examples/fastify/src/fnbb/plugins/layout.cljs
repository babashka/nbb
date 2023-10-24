(ns fnbb.plugins.layout)

(defn layout
  [children & {:keys [title] :or {title "Fastify nbb Example"}}]
  [:html {:lang "en"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:content "width=device-width, initial-scale=1.0" :name "viewport"}]
    [:script {:src "https://unpkg.com/htmx.org@1.9.6"}]
    [:script {:src "https://unpkg.com/htmx.org/dist/ext/json-enc.js"}]
    [:link {:rel "icon" :type "image/png" :href "/public/favicon.png"}]
    [:link {:rel "stylesheet" :href "/public/main.css"}]
    [:title title]]
   [:body children]])
