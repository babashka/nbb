# Bun

According to their [website](https://github.com/Jarred-Sumner/bun), bun is an

> Incredibly fast JavaScript runtime, bundler, transpiler and package manager – all in one.

This document contains best practices to use nbb with bun. Bun is still experimental and does not support the full Node API. As such, some things may not work as expected.

## Evaluate a file

Write an `index.mjs` like this:

```
// loadFile is silently failing in bun, so we use loadString instead:
import { loadString } from 'nbb'

const code = await Bun.file('index.cljs').text();

await loadString(code);
```

Since loadFile is using Node's fs API and this is not fully supported yet, we work around this using `loadString`.

With `index.cljs` as:

```
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
