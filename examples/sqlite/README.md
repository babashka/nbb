# sqlite3 example

Install the `sqlite` and `sqlite3` dep by running `npm install`.

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

For run the `honeysql.cljs` example, using the [HoneySQL]() library, you can do:

```
$ classpath="$(clojure -A:nbb -Spath -Sdeps '{:aliases {:nbb {:replace-deps {com.github.seancorfield/honeysql {:git/tag "v2.0.0-rc5" :git/sha "01c3a55"}}}}}')"
$ nbb --classpath "$classpath" honeysql.cljs
```
