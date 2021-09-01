# Nbb

Not [babashka](https://babashka.org/). Node.js babashka!?

Ad-hoc CLJS scripting on Node.js.

## Status

Experimental. Please report issues [here](https://github.com/borkdude/nbb/issues).

## Goals and features

Nbb's main goal is to make it _easy_ to get started with ad hoc CLJS scripting
on Node.js.

Additional goals and features are:

- Fast startup without relying on a custom version of Node.js.
- Small artifact (current size is around 1.2MB).
- First class [macros](#macros).
- Support building small TUI apps using [Reagent](#reagent).
- Complement [babashka](https://babashka.org/) with libraries from the Node.js ecosystem.

## Community

- Submit bugs at [Github Issues](https://github.com/borkdude/nbb/issues).
- Join [Github Discussions](https://github.com/borkdude/nbb/discussions/categories) for proposing ideas, show and tell and Q&A.
-  Join the [channel](https://app.slack.com/client/T03RZGPFR/C029PTWD3HR) on Clojurians Slack.
- Follow news or tweet using the Twitter hashtag
  [#nbbjs](https://twitter.com/hashtag/nbbjs?f=live).

## Requirements

Nbb requires Node.js v12 or newer.

## How does this tool work?

CLJS code is evaluated through [SCI](https://github.com/borkdude/sci), the same
interpreter that powers [babashka](https://babashka.org/). Because SCI works
with advanced compilation, the bundle size, especially when combined with other
dependencies, is smaller than what you get with self-hosted CLJS. That makes
startup faster. The trade-off is that execution is less performant and that only
a subset of CLJS is available (e.g. no deftype, yet).

## Usage

Install `nbb` from NPM:

```
$ npm install nbb -g
```

Omit `-g` for a local install.

Try out an expression:

``` clojure
$ nbb -e '(+ 1 2 3)'
6
```

And then install some other NPM libraries to use in the script. E.g.:

```
$ npm install csv-parse shelljs zx
```

Create a script which uses the NPM libraries:

``` clojure
(ns script
  (:require ["csv-parse/lib/sync$default" :as csv-parse]
            ["fs" :as fs]
            ["shelljs$default" :as sh]
            ["zx$fs" :as zxfs]
            [nbb.core :refer [*file*]]))

(println (count (str (fs/readFileSync "script.cljs"))))

(prn (sh/ls "."))

(prn (csv-parse "foo,bar"))

(prn (zxfs/existsSync *file*))
```

Call the script:

```
$ nbb script.cljs
264
#js ["CHANGELOG.md" "README.md" "bb.edn" "deps.edn" "main.js" "node_modules" "out" "package-lock.json" "package.json" "shadow-cljs.edn" "src" "test.cljs"]
#js [#js ["foo" "bar"]]
true
```

## Macros

Nbb has first class support for macros: you can define them right inside your `.cljs` file, like you are used to from JVM Clojure. Consider the `plet` macro to make working with promises more palatable:


``` clojure
(defmacro plet
  [bindings & body]
  (let [binding-pairs (reverse (partition 2 bindings))
        body (cons 'do body)]
    (reduce (fn [body [sym expr]]
              (let [expr (list '.resolve 'js/Promise expr)]
                (list '.then expr (list 'clojure.core/fn (vector sym)
                                        body))))
            body
            binding-pairs)))
```

Using this macro we can look async code more like sync code. Consider this puppeteer example:

``` clojure
(-> (.launch puppeteer)
      (.then (fn [browser]
               (-> (.newPage browser)
                   (.then (fn [page]
                            (-> (.goto page "https://clojure.org")
                                (.then #(.screenshot page #js{:path "screenshot.png"}))
                                (.catch #(js/console.log %))
                                (.then #(.close browser)))))))))
```

Using `plet` this becomes:

``` clojure
(plet [browser (.launch puppeteer)
       page (.newPage browser)
       _ (.goto page "https://clojure.org")
       _ (-> (.screenshot page #js{:path "screenshot.png"})
             (.catch #(js/console.log %)))]
      (.close browser))
```

See the [puppeteer
example](https://github.com/borkdude/nbb/blob/main/examples/puppeteer/example.cljs)
for the full code.

Since v0.0.36, nbb includes [promesa](#promesa) which is a library to deal with
promises. The above `plet` macro is similar to `promesa.core/let`.

## Startup time

``` clojure
$ time nbb -e '(+ 1 2 3)'
6
nbb -e '(+ 1 2 3)'   0.17s  user 0.02s system 109% cpu 0.168 total
```

The baseline startup time for a script is about 170ms seconds on my laptop. When
invoked via `npx` this adds another 300ms or so, so for faster startup, either
use a globally installed `nbb` or use `$(npm bin)/nbb script.cljs` to bypass
`npx`.

## Dependencies

### NPM dependencies

Nbb does not depend on any NPM dependencies. All NPM libraries loaded by a
script are resolved relative to that script. When using the [Reagent](#reagent)
module, React is resolved in the same way as any other NPM library.

### Classpath

To load `.cljs` files from local paths or dependencies, you can use the
`--classpath` argument. The current dir is added to the classpath automatically.
So if there is a file `foo/bar.cljs` relative to your current dir, then you can
load it via `(:require [foo.bar :as fb])`. Note that `nbb` uses the same naming
conventions for namespaces and directories as other Clojure tools: `foo-bar` in
the namespace name becomes `foo_bar` in the directory name.

To load dependencies from the Clojure ecosystem,
you can use the Clojure CLI or babashka to download them and produce a classpath:

``` clojure
$ classpath="$(clojure -A:nbb -Spath -Sdeps '{:aliases {:nbb {:replace-deps {com.github.seancorfield/honeysql {:git/tag "v2.0.0-rc5" :git/sha "01c3a55"}}}}}')"
```
and then feed it to the `--classpath` argument:

``` clojure
$ nbb --classpath "$classpath" -e "(require '[honey.sql :as sql]) (sql/format {:select :foo :from :bar :where [:= :baz 2]})"
["SELECT foo FROM bar WHERE baz = ?" 2]
```

Currently `nbb` only reads from directories, not jar files, so you are
encouraged to use git libs. Support for `.jar` files will be added later.

## Current file

The name of the file that is currently being executed is available via
`nbb.core/*file*` or on the metadata of vars:

``` clojure
(ns foo
  (:require [nbb.core :refer [*file*]]))

(prn *file*) ;; "/private/tmp/foo.cljs"

(defn f [])
(prn (:file (meta #'f))) ;; "/private/tmp/foo.cljs"
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
  (:require ["ink" :refer [render Text]]
            [reagent.core :as r]))

(defonce state (r/atom 0))

(doseq [n (range 1 11)]
  (js/setTimeout #(swap! state inc) (* n 500)))

(defn hello []
  [:> Text {:color "green"} "Hello, world! " @state])

(render (r/as-element [hello]))
```

<img src="img/ink.gif"/>

## Promesa

Working with callbacks and promises can become tedious. Since nbb v0.0.36 the
`promesa.core` namespace is included with the `let` and `do!` macros. An example:

``` clojure
(ns prom
  (:require [promesa.core :as p]))

(defn sleep [ms]
  (js/Promise.
   (fn [resolve _]
     (js/setTimeout resolve ms))))

(defn do-stuff
  []
  (p/do!
   (println "Doing stuff which takes a while")
   (sleep 1000)
   1))

(p/let [a (do-stuff)
        b (inc a)
        c (do-stuff)
        d (+ b c)]
  (prn d))
```

``` clojure
$ nbb prom.cljs
Doing stuff which takes a while
Doing stuff which takes a while
3
```

Also see [API docs](api.md).

## Examples

See the [examples](examples) directory for small examples.

Also check out these projects built with nbb:

- [c64core](https://github.com/chr15m/c64core): retrocompute aesthetics twitter bot
- [gallery.cljs](https://gist.github.com/borkdude/05c4f4ce81a988f90917dc295d8f306e): script to download walpapers from [windowsonearth.org](https://www.windowsonearth.org).

## API

See [API](doc/api.md) documentation.

## Migrating to shadow-cljs

See this
[gist](https://gist.github.com/borkdude/7e548f06fbefeb210f3fcf14eef019e0) on how
to convert an nbb script or project to
[shadow-cljs](https://github.com/thheller/shadow-cljs).

## Build

Prequisites:

- [babashka](https://babashka.org/) >= 0.4.0
- [Clojure CLI](https://clojure.org/guides/getting_started#_clojure_installer_and_cli_tools) >= 1.10.3.933
- Node.js 16.5.0 (lower version may work, but this is the one I used to build)

To build:

- Clone and cd into this repo
- `bb release`

Run `bb tasks` for more project-related tasks.

## License

Copyright © 2021 Michiel Borkent

Distributed under the EPL License. See LICENSE.
