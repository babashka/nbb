# Testing

For testing nbb code you can use `cljs.test` which is built into nbb.

You can find examples of this in `example.cljs`.

You can run the tests with `nbb -m example/run-tests`.

By default, failed tests fail the current process. To disable this behavior:

```clojure
(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m])
```
