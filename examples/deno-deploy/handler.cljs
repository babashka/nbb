(ns handler
  ;; @hono/hono is imported natively and registered in main.ts, so we
  ;; require it here by the bare name rather than a "jsr:" specifier.
  (:require ["@hono/hono" :refer [Hono]]))

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
