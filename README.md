# TBD

A babashka like thing for nodeJS.

## Usage

To build locally:

- Clone and cd into this repo
- `bb release`
- `npm install -g`

Then from some other dir:

- `npm install shelljs`

Make script:

``` clojure
(def sh (js/require "shelljs"))

(-> sh (.cd  ".."))
(-> sh .ls prn)
(.cd sh "foobar")
(-> sh .ls prn)
```

Call the script:

```
$ tbd script.cljs
#js ["4ever-clojure""clj-async-profiler" "clj-sqlite-graalvm-native" "clojure-lsp" ...]
#js ["node_modules" "package-lock.json" "package.json" "script.cljs"]
```

## License

TBD.
