# Malli

You can load malli from source with the `deps.edn` in this directory, as follows:

```
$ npm install
$ node node_modules/.bin/nbb -cp $(clojure -Spath) -e "(require '[malli.core :as m]) (m/validate [:int] 1)"
true
```
