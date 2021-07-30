# TBD

A babashka like thing for nodeJS.

## Status

Experimental.

## Usage

To build locally:

- Clone and cd into this repo
- `bb release`
- `npm install -g`

Then from some other dir, install some NPM libraries to use in the script. E.g.:

```
$ npm install csv-parse
$ npm install shelljs
```

and suppose you have some local JS file `foo.js` with `console.log("hello");`.

Create a script which uses the NPM libraries:

``` clojure
(ns script
  (:require ["./foo.js"]
            ["csv-parse/lib/sync.js" :default csv-parse]
            ["fs" :as fs]
            ["shelljs" :default sh]))

(println (count (str (.readFileSync fs "script.cljs"))))

(prn (.ls sh "."))

(prn (csv-parse "foo,bar"))
```

Call the script:

```
$ tbd script.cljs
hello
264
#js ["CHANGELOG.md" "README.md" "bb.edn" "deps.edn" "main.js" "node_modules" "out" "package-lock.json" "package.json" "shadow-cljs.edn" "src" "test.cljs"]
#js [#js ["foo" "bar"]]
```

The script takes about 200ms seconds to run on my laptop.

## License

TBD.
