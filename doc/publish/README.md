# Publishing an nbb project to npm

This document describes how to publish an nbb based project to npm. As an
example we will build a CLI, `print-cli-args` that prints command line
arguments.

First, create a new directory `print-cli-args` and cd into it. Then create a `package.json`:

``` json
{
  "name": "print-cli-args",
  "version": "0.0.1",
  "dependencies": {
    "nbb": "0.0.109"
  },
  "bin": {
    "print-cli-args": "index.mjs"
  }
}
```

The CLI depends on a specific version of nbb and exposes itself as a binary
called `print-cli-args`, which is linked to `index.mjs` in our project. It is
important to use `.mjs` rather than `.js` so Node.js recognizes the file as an
ES6 module.

The `index.mjs` file is a small wrapper that sets up the classpath for nbb to
the `src` directory relative to the wrapper using `addClassPath`. It also calls
the initial CLJS file using `loadFile`.

``` javascript
#!/usr/bin/env node

import { addClassPath, loadFile } from 'nbb';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __dirname = fileURLToPath(dirname(import.meta.url));

addClassPath(resolve(__dirname, 'src'));
await loadFile(resolve(__dirname, 'src/print_cli_args/core.cljs'));
```

Finally, in `src/print_cli_args/core.cljs` we write the CLJS code:

``` clojure
(ns print-cli-args.core
  (:require [clojure.string :as str]))

(def cmd-line-args (not-empty (js->clj (.slice js/process.argv 2))))

(println "Your command line arguments:"
         (or (some->> cmd-line-args (str/join " "))
             "None"))
```

To test the CLI in development, run `node index.mjs 1 2 3`.

When you `npm install -g` from within the project, you can call `print-cli-args`
from anywhere on your system.

When everything looks good, it's time to `npm publish` so everyone can enjoy
your new CLI.

After you have done so, you can run this example from npm using:

``` shell
$ npx print-cli-args 1 2 3
Your command line arguments: 1 2 3
```

or:

``` shell
$ npm install -g print-cli-args
$ print-cli-args 1 2 3
Your command line arguments: 1 2 3
```
