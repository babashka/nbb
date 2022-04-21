# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## 0.3.x

- Include `clojure.data` as built-in dependency
- Add `datascript.core/squuid`
- Add support for different CLI name

## 0.3.8

- Nbb is now able to run [medley](https://github.com/weavejester/medley) from source

## 0.3.7

- Use `import-meta-resolve` to discover ES module files when `createRequire` doesn't find them.
  Fixes issue with loading newer version of `zx` (6.0.0+).

- Add optional datascript tests

## 0.3.6

- Allow `reagent.ratom` to be required separately

## 0.3.5

- Add initial features support with datascript and datascript-transit
- Include `cognitect.transit` as built-in dependency
- Add `clojure.pprint/print-table`

## 0.3.4

- Don't load modules more than once

## 0.3.3

- Fix [#158](https://github.com/babashka/nbb/issues/158): be able to use `cljs.test` as namespace name + support `:refer-macros`

## 0.3.2

- Fix [#154](https://github.com/babashka/nbb/issues/154): ignore `:require-macros`

## 0.3.1

- Fix [#139](https://github.com/babashka/nbb/issues/139): include `goog.string/format`
- Support `*print-err-fn*`

## 0.3.0

- Include `org.clojure/tools.cli` as built-in dependency.
- Update SCI to `0.3.3`

## 0.2.9

- Add `nbb.repl/get-completions` for `inf-clojure`

## 0.2.8

- Feat [#149](https://github.com/babashka/nbb/issues/149): Add auto-completions to normal REPL

## 0.2.7

- Support property access completions in nREPL server

## 0.2.6

- Fix [#146](https://github.com/babashka/nbb/issues/146): add JS completions for clojure functions in nREPL server

## 0.2.5

- Fix [#146](https://github.com/babashka/nbb/issues/146): add var completions for clojure functions in nREPL server

## 0.2.4

- Re-implement `require` via automatic `await`, allows conditional requires like: `(when foo? (require 'bar))`

## 0.2.3

- feat [#142](https://github.com/babashka/nbb/issues/142): experimental nbb.core/await

## 0.2.2

- Update SCI configs with `promesa/do` + `promesa/create`

## 0.2.1

- Bump SCI: drop location metadata from symbols, except top level

## 0.2.0

- Bump promesa for built-in clj-kondo config and `with-redefs` macro ([@niwinz](https://github.com/niwinz))
- Include new `with-redefs` macro ([@eccentric-j](https://github.com/eccentric-j))

## 0.1.9

- Fix [#135](https://github.com/babashka/nbb/issues/135): expose `promesa/run!`
- Fix [#131](https://github.com/babashka/nbb/issues/131): delayed printing in nREPL

## 0.1.8

- Fix bug: allow arbitrary expression in class position in `new` (part 2)

## 0.1.7

- Fix bug: allow arbitrary expression in class position in `new`
- Bump SCI
- Bump promesa for built-in clj-kondo config

## 0.1.6

- Show nbb version at REPL startup ([@prestancedesign](https://github.com/prestancedesign))
- Bump promesa with new `->` and `->>` macros ([@prestancedesign](https://github.com/prestancedesign))
- Bump SCI with performance improvements (4x) for loops

## 0.1.5

- Fix: use `Reflect.construct` to fix interop with Graal Node.js

## 0.1.4

- Fix [#118](https://github.com/babashka/nbb/issues/118): Use `Reflect.apply` to fix interop with Graal Node.js

## 0.1.3

- Support `:as-alias`

## 0.1.2

- Bump SCI and cljs-bean
- CIDER improvements ([@benjamin-asdf](https://github.com/benjamin-asdf))

## 0.1.1

- Fix [#115](https://github.com/babashka/nbb/issues/115): Missing `cljs.core` functions: `array?`, `object?`, `js-delete`, `undefined?`
- Fix [#114](https://github.com/babashka/nbb/issues/114): When non-Error values are thrown they aren't be caught by `:default`

## 0.1.0

- Add [cljs-bean](https://github.com/mfikes/cljs-bean) to ease JS interop
- Add `--help` option to print help text
- Add `--main` option to invoke main function

## 0.0.117

- Support binding `clojure.test/report` [#106](https://github.com/babashka/nbb/issues/106)

## 0.0.117

- Expose `Math` directly

## 0.0.113

- Add `tap` and related functions

## 0.0.112

- Expose more `promesa` functions

## 0.0.110

- Bump SCI

## 0.0.109

- Add `nbb.classpath` namespace and JS API for adding classpath entries

## 0.0.108

- Support `test-vars` and `use-fixtures` in `clojure.test` namespace
  [#99](https://github.com/babashka/nbb/issues/99)

## Breaking changes

None yet.
