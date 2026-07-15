(ns handler
  ;; registered in main.ts, so required by bare name (not "jsr:@hono/hono")
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

;; main.ts re-exports this as the module default so Deploy finds fetch
#js {:fetch (.-fetch app)}
