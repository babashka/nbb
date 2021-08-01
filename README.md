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
  (:require ["csv-parse/lib/sync" :as csv-parse]
            ["fs" :as fs]
            ["shelljs" :as sh]))

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

## Startup time

The baseline startup time for a script is about 200ms seconds on my
laptop. Ufortunately `npx` adds another 300ms or so.
To get faster startup time for a local `nbb`, use `$(npm bin)/nbb script.cljs`,
or install `nbb` globally..

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
