# Dependencies

Currently nbb doesn't load code from .jar files and doesn't have configuration
file like babashka's `bb.edn`. It does however have the ability to load
dependencies from directories. Here we describe how you can use babashka to load
dependencies from clojars.

## Create an nbb.edn file

This file will contain the dependencies, similar to `bb.edn` for babashka.
E.g.:

``` clojure
{:deps {com.github.seancorfield/honeysql {:mvn/version "2.2.868"}}}
```

## Create a bb.edn file

Next, we will create a `bb.edn` file to download these deps and bundle them into
an uberjar. Also, we're going to unzip that uberjar into a directory,
`nbb-deps`.

``` clojure
{:tasks
 {:requires ([babashka.fs :as fs])
  write-deps {:doc "Write dependencies in deps folder"
              :task (do (shell "bb --config nbb.edn uberjar nbb-deps.jar")
                        (fs/create-dirs "nbb-deps")
                        (fs/unzip "nbb-deps.jar" "nbb-deps"))}}}
```

Run `bb write-deps` and the dependencies will be available in the `nbb-deps` directory.

## Create a deps.cljs file

In the `deps.cljs` file we will add the `nbb-deps` directory to nbb's classpath:

``` clojure
(ns deps
  (:require [nbb.classpath :refer [add-classpath]]))

(add-classpath "nbb-deps")
```

## Create your script

Now we can write our final script:

``` clojure
(ns script
  (:require [deps]
            [honey.sql :as sql]))

(prn (sql/format {:select :* :from :dude}))
```

First we load the `deps` namespace which will set the classpath and directly
after that, we can load dependencies like we are used to in babashka.

Now run the script:

``` clojure
$ nbb script.cljs
["SELECT * FROM dude"]
```

It worked!
