# Malli

This example shows how you can use [malli](https://github.com/metosin/malli)
with nbb. 

To run this example, run `npm install` and then `npx nbb
example.cljs` (so Node will use the nbb version of this project).

Suggestion to decide on the version of malli:
First install the tools [bbin](https://github.com/babashka/bbin) and [deps-info](https://github.com/rads/deps-info).
Then run the following to get the latest tag for the latest tag:
```
deps-info-infer --lib borkdude/malli --latest-sha
#:borkdude{malli #:git{:url "https://github.com/borkdude/malli", :sha "e400bbcdab09b21dc42d82f90400f60f85af6bd9"}}
```
