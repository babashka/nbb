# TBD

A babashka like thing for nodeJS.

## Status

Experimental.

## Usage

To build locally:

- Clone and cd into this repo
- `bb release`
- `npm install -g`

Then from some other dir, install some NPM library to use in a script, for example `shelljs`:

```
$ npm install shelljs
```

Create a script:

``` clojure
(def sh (js/require "shelljs"))

(defmacro $ [op & args]
  (list* (symbol (str "." op)) 'sh args))

(prn (str ($ which "git")))
(prn (str ($ pwd)))
($ cd  "..")
(-> ($ ls) prn)
($ cd "foobar")
(-> ($ ls) prn)
```

Call the script:

```
$ tbd script.cljs
"/usr/bin/git"
"/private/tmp/foobar"
#js ["4ever-clojure""clj-async-profiler" "clj-sqlite-graalvm-native" "clojure-lsp" ...]
#js ["node_modules" "package-lock.json" "package.json" "script.cljs"]
```

The script takes about 150-200ms seconds to run on my laptop.

Another example using `csv-parse`:

```
$ npm install csv-parse
```

Script:

``` clojure
(def csv-parse (js/require "csv-parse/lib/sync"))

(-> (csv-parse "foo,bar,baz\n1,2,3" #js {:columns true})
    (js->clj :keywordize-keys true)
    prn)
```

Call the script:

```
$ tbd script.cljs
[{:foo "1", :bar "2", :baz "3"}]
```

## License

TBD.
