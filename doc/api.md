# API

In addition to `clojure.core`, `clojure.set`, `clojure.edn`, `clojure.string`,
`clojure.walk` and `clojure.pprint`, nbb exposes the following namespaces:

## Nbb

### nbb.core

- `*file*`: dynamic var representing the currently executing file.
- `(load-file f)`: reads contents of file f and then handles it using `load-string`.
- `(load-string s)`: asynchronously parses and evaluates `s`, returns result as promise.
- `(slurp f)`: asynchronously slurps contents of file to string, returns result as promise.

### nbb.repl

- `(repl), (repl opts)`: starts REPL which reads from stdin and prints to
  stdout. Returns promise which is resolved when input is closed.
- `(socket-repl), (socket-repl opts)`: starts REPL which reads from socket and
  writes to socket. Accepts optional `opts` map with `:port`. Returns promise
  which is resolved when input is closed.

## Promesa

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).

### promesa.core

- `let`, `do!`

## Reagent

See [reagent docs](http://reagent-project.github.io/docs/master/).

### reagent.core

- `atom`, `as-element`, `with-let`, `cursor`, `create-class`, `create-compiler`

### reagent.ratom

- `with-let-values`, `reactive?`

### reagent.dom.server

- `render-to-string`, `render-to-static-markup`

## Js-interop

See [js-interop docs](https://github.com/applied-science/js-interop).

### applied-science.js-interop

- `get`, `get-in`, `contains?`, `select-keys`, `lookup`, `assoc!`, `assoc-in!`,
  `update!`, `extend!`, `push!`, `unshift!`, `call`, `apply`, `call-in`,
  `apply-in`, `obj`, `let`, `fn`, `defn`, `lit`

## Tools.cli

### clojure.tools.cli

- `format-lines`, `summarize`, `get-default-options`, `parse-opts`, `make-summary-part`

## transit-cljs

### cognitect.transit

- `write`, `writer`, `write-handler`, `write-meta`, `read`, `read`, `read-handler`, `tagged-value`
