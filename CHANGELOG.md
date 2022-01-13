# Changelog

For a list of breaking changes, check [here](#breaking-changes).

## 0.1.1

- Fix #115: Missing `cljs.core` functions: `array?`, `object?`, `js-delete`, `undefined?`
- Fix #114: When non-Error values are thrown they aren't be caught by `:default`

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
