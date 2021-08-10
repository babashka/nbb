# API

In addition to `clojure.core`, `clojure.set`, `clojure.edn`, `clojure.string`,
`clojure.walk`, nbb exposes the following namespaces:

## Nbb

### nbb.core

- `*file*`: dynamic var representing the currently executing file.
- `(load-file f)`: reads contents of file f and then handles it using `load-string`.
- `(load-string s)`: asynchronously parses and evaluates `s`, returns result as promise.
- `(slurp f)`: asynchronously slurps contents of file to string, returns result as promise.

## Promesa

See [promesa docs](https://cljdoc.org/d/funcool/promesa/6.0.2/doc/user-guide).]

### promesa.core

- `let`, `do!`

## Reagent

See [reagent docs](http://reagent-project.github.io/docs/master/).

### reagent.core

- `atom`, `as-element`

### reagent.dom

- `render`
