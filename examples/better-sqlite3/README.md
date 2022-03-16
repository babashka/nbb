# Synchronous sqlite3 example using `better-sqlite3`

While [node-sqlite3](https://github.com/mapbox/node-sqlite3) is asynchronous and non-blocking,
[better-sqlite3](https://github.com/JoshuaWise/better-sqlite3) provides a synchronous API - which may be simpler and
faster for many typical SQLite use cases.


# Installation

Install `better-sqlite3` by running `npm install`.

# example.cljs

A `better-sqlite3`-based version of `examples/sqlite/example.cljs`.

```
$ nbb example.cljs
1: 0
2: 1
3: 2
4: 3
5: 4
6: 5
7: 6
8: 7
9: 8
10: 9
```

