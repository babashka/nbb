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

Now we're going to package up `out.mjs` to a standalone JS file:

```
$ npx ncc build out.mjs -m
```

This produces a file `dist/index.mjs` which can run without `node_modules`.
