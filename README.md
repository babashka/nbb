# TBD

A babashka like thing for nodeJS.

## Status

Experimental.

## Usage

To build locally:

- Clone and cd into this repo
- `bb release`
- `npm install -g`

Then from some other dir, install some NPM library to use in a script:

```
$ npm install shelljs
```

Create a script:

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

The script takes about 150-200ms seconds to run on my laptop.

## License

TBD.
