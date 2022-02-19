# Changelog

For a list of breaking changes, check [here](#breaking-changes).

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
