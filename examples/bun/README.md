# Bun examples

To run example, invoke `bun run --bun nbb <example.cljs>`.

## FFI

The [ffi.cljs](ffi.cljs) file shows an FFI example using ncurses. You can
install ncurses on mac using `brew install ncurses`. See it in action in
[this](https://twitter.com/borkdude/status/1551681759716282369) tweet.

## Blipgloss

See it in action in [this tweet](https://twitter.com/borkdude/status/1551685556106629125).

## Sqlite

[sqlite.cljs](sqlite.cljs) shows working with bun's [sqlite3-driver](https://bun.sh/docs/api/sqlite).

For interactive development, make sure to run the nrepl-server using bun:

```shell
bun run --bun nbb nrepl-server
```

## Single-file executable

``` shell
bun run --bun nbb bundle <example.cljs> -o out.mjs
bun build out.mjs --compile --outfile cli
```
