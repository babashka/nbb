# Nbb

Not [babashka](https://babashka.org/). Node.js babashka!?

Previously known as TBD and Nodashka.

## Status

Experimental.

## Usage

Install `nbb` from NPM:

```
$ npm install nbb
```

and perhaps some other NPM libraries to use in the script. E.g.:

```
$ npm install csv-parse
$ npm install shelljs
```

Create a script which uses the NPM libraries:

``` clojure
(ns script
  (:require ["csv-parse/lib/sync.js" :default csv-parse]
            ["fs" :as fs]
            ["shelljs" :default sh]))

(println (count (str (.readFileSync fs "script.cljs"))))

(prn (.ls sh "."))

(prn (csv-parse "foo,bar"))
```

Call the script:

```
$ npx nbb script.cljs
264
#js ["CHANGELOG.md" "README.md" "bb.edn" "deps.edn" "main.js" "node_modules" "out" "package-lock.json" "package.json" "shadow-cljs.edn" "src" "test.cljs"]
#js [#js ["foo" "bar"]]
```

## Startup time

The above script takes about 200ms seconds to run on my laptop when ran as a
globally installed `nbb`, but unfortunately `npx` adds another 300ms or so.

To get faster startup time for a local `nbb`, use `$(npm bin)/nbb script.cljs`.

## Reagent

Nbb includes `reagent.core` which will be lazily loaded when required. You
can use this together with [ink](https://github.com/vadimdemedes/ink) to create
a TUI application:

```
$ npm install ink
```

`ink-demo.cljs`:
``` clojure
(ns ink-demo
  (:require [reagent.core :as r]
            ["ink" :refer [render Text]]))

(defonce state (r/atom 0))
(doseq [n (range 1 11)]
  (js/setTimeout #(swap! state inc) (* n 500)))

(defn hello []
  [:> Text {:color "green"} "Hello, world! " @state])

(render (r/as-element [hello]))
```

<img src="img/ink.gif"/>

## Problems

If you are a JS expert, which I am not, and you have some insights to the
following issues, feel free to reach out via Github Discussions or the
`#nbb` channel on Clojurians Slack.

- Using `npx` to start `nbb` is slow. Nbb itself only takes 180ms or
  so to start. But `npx` adds another 300ms or so to it. It seems `$(npm
  bin)/nbb script.cljs` is a way to get better startup time.
- Can a global install of `nbb` be combined with local dependencies? How?

## Require syntax and rules

Nbb adopts the syntax that `shadow-cljs` and CLJS provide for requiring NPM
libraries. It also supports the non-CLJS-standard `:default` option which is
only supported by `shadow-cljs`. Requiring libraries can only be done through a
top level `ns` form and/or one or more top-level `require` forms. Additionally,
`nbb` does not support `js/require`, but it does allow `js/import` (dynamic
import).

## Build

Prequisites:

- [babashka](https://babashka.org/) >= 0.4.0
- [Clojure CLI](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools) >= 1.10.3.933
- Node.js 16.5.0 (lower version may work, but this is the one I used to build)

To build:

- Clone and cd into this repo
- `bb release`

## License

Copyright © 2021 Michiel Borkent

Distributed under the EPL License. See LICENSE.
