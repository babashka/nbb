# rqlite-js example

This example shows how to run a minimalistic environment with distributed database, not a production use. For production setup you should check [rqlite on Github](https://github.com/rqlite/rqlite). There are plenty of useful tips out there.

Let's look at how to have a simple setup on MacOS.

```sh
brew install rqlite
```

To have a single node instance you've to run at least:

```sh
rqlited -node-id 1 ~/node.1
```

Quite same instructions actually apply on Linux and even on Windows.

We also need to insert some data so this example project can demonstrate some functionality.

```
$ rqlite
127.0.0.1:4001> CREATE TABLE foo (id INTEGER NOT NULL PRIMARY KEY, name TEXT)
0 row affected (0.000668 sec)
127.0.0.1:4001> .schema
+-----------------------------------------------------------------------------+
| sql                                                                         |
+-----------------------------------------------------------------------------+
| CREATE TABLE foo (id INTEGER NOT NULL PRIMARY KEY, name TEXT)               |
+-----------------------------------------------------------------------------+
127.0.0.1:4001> INSERT INTO foo(name) VALUES("fiona")
1 row affected (0.000080 sec)
127.0.0.1:4001> SELECT * FROM foo
+----+-------+
| id | name  |
+----+-------+
| 1  | fiona |
+----+-------+
```

