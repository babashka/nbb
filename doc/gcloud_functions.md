# Nbb on Google Cloud Functions

This article is based on [this](https://gist.github.com/jackrusher/1cc61e0ca0e929b9ec21bf4407af6d75) gist by Jack Rusher. Thanks Jack!

## Creating an nbb google cloud function

All you need to do to get nbb running on GCP Cloud Functions is the following:

package.json:
``` json
{
    "type": "module",
    "scripts": {
        "start": "functions-framework --target=hello"
    },
    "main": "index.mjs",
    "dependencies": {
        "nbb": "~1.2.174",
        "@google-cloud/functions-framework": "~3.2.0"
    }
}
```

index.mjs:
``` javascript
import { loadFile } from 'nbb';

const { hello } = await loadFile('./hello.cljs');

export { hello }
```

hello.cljs:
``` clojure
(ns hello)

(defn hello [req res]
  (js/console.log req)
  (.send res "hello world"))

;; export
#js {:hello hello}
```

Then deploy the function with:

```
$ gcloud functions deploy hello --runtime nodejs20 --trigger-http
```

Also see [Nbb on AWS Lambda](aws_lambda.md).
