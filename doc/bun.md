# Bun

According to their [website](https://github.com/Jarred-Sumner/bun), bun is an

> Incredibly fast JavaScript runtime, bundler, transpiler and package manager – all in one.

This document contains best practices to use nbb with bun. Bun is still experimental and does not support the full Node API. As such, some things may not work as expected.

## Installing nbb

`package.json`:

```
{
  "dependencies": {
    "nbb": "^0.5.121"
  }
}
```

Run `bun install` to install nbb.

## JS wrapper

To run scripts with bun, you need to write a JS wrapper, else bun is going to
invoke Node.js, since the nbb entrypoint has a Node.js shebang.

Write an `index.mjs` like this:

``` javascript
import { loadFile } from 'nbb'
await loadFile('index.cljs')
```

With `index.cljs` as:

``` clojure
(defn foo []
  (prn :hello))

(foo)
```

you can now run:

```
bun run index.mjs
```

and you should see:

```
:hello
```

For faster startup time, run `bun bun index.mjs`.

## Examples

See [examples/bun](../examples/bun) for bun examples.
