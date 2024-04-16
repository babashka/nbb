# Bun

According to their [website](https://bun.sh/), bun is an

> Incredibly fast JavaScript runtime, bundler, transpiler and package manager – all in one.

This document contains best practices to use nbb with bun. Bun is still experimental and does not support the full Node API. As such, some things may not work as expected.

## Installing nbb

<sub>Tested with bun v1.1.3</sub>

```shell
$ bun add nbb
```

which should yield a `package.json` like:

```
{
  "dependencies": {
    "nbb": "^1.2.187"
  }
}
```

Test it:

```sh
$ bun run --bun nbb -e '(prn "Hello World!")'
"Hello World!"
```

With `index.cljs` as:

``` clojure
(defn foo []
  (prn :hello))

(foo)
```

you can now run:

```shell
$ bun run --bun nbb index.cljs
```

and you should see:

```
:hello
```

## Examples

See [examples/bun](../examples/bun) for bun examples.
