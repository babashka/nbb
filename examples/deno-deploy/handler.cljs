(ns handler
  (:require ["jsr:@hono/hono" :refer [Hono]]))

(def app (Hono.))

(.get app "/"
      (fn [ctx]
        (.text ctx "Hello from nbb on Deno Deploy!")))

(.get app "/hello/:name"
      (fn [ctx]
        (let [name (.. ctx -req (param "name"))]
          (.json ctx #js {:hello name
                          :runtime "nbb + deno"}))))

;; Returned to the JS wrapper (main.ts), which re-exports it as the
;; module's default export so Deno Deploy / `deno serve` can find `fetch`.
#js {:fetch (.-fetch app)}
