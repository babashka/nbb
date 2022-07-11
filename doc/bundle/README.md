# Bundle

Since version 0.6.122, nbb supports the `bundle` command, which will bundle an
nbb application to a single JS file. You can then process this JS file with a
tool like [ncc](https://github.com/vercel/ncc) to get a truly standalone JS file
with all dependencies included. You can run the following example in this
directory, provided you have run `npm install`.

To run `example.cljs` normally, we would run: `npx nbb example.cljs`.

To produce a single JS file suitable for JS bundlers, run:

```
$ npx nbb bundle example.cljs -o out.mjs
```

You can already run this file with Node.js, but it still requires the installed
`node_modules`:

```
$ node out.mjs
```

## Ncc

Now we're going to package up `out.mjs` to a standalone JS file using `ncc`:

```
$ npx ncc build out.mjs -m
```

This produces a file `dist/index.mjs` which can run without `node_modules`.

See an example [Github Action](https://github.com/borkdude/nbb-action-example) written using nbb which uses the bundle command.

## Webpack

Ncc produces a `dist/index.mjs` file, since the input was also an `.mjs`
file. Unfortunately, making an executable file with a shebang is not well
supported with `.mjs` files. However, webpack is able to produce a regular `.js`
file (non-ESM) from our `out.mjs`. Also, `ncc` seems to have problems with the [ink](https://github.com/vadimdemedes/ink) package, while `webpack` can handle it.

Run:

```
$ npx webpack --config webpack.prod.js
```

which produces a `dist/index.js`. You can then prepend a `#!/usr/bin/env node`
shebang to this file, make it executable and then distribute it. Or you can use
[pkg](https://github.com/vercel/pkg) to make a self-contained executable.

On Windows, you can use `npm install -D wrap-cmd` and then `npx wrap-cmd
dist/index.js` to create a wrapper `.cmd` file.

## Rollup

For completeness, a rollup config is also provided.
Produce the `dist/index.mjs` file with:

```
$ node ./node_modules/.bin/rollup -c rollup.config.js
```

## Pkg

The `dist/index.js` produced by Webpack can be used with [pkg](https://github.com/vercel/pkg) to create a standalone executable:

```
$ npx pkg -t node16 dist/index.js -o mytool
$ ./mytool
```

See the [caxa](https://github.com/babashka/nbb/tree/main/doc/caxa) docs for an
alternative approach to building a standalone executable without a bundler.
