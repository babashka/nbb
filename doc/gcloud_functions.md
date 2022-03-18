# Nbb on Google Cloud Functions

This article is based on [this](https://gist.github.com/jackrusher/1cc61e0ca0e929b9ec21bf4407af6d75) gist by Jack Rusher. Thanks Jack!

## Creating an nbb google cloud function

All you need to do to get nbb running on AWS Lambda is the following:

package.json:
``` json
{
    "type": "module",
    "scripts": {
        "start": "functions-framework --target=hello"
    },
    "main": "index.mjs",
    "dependencies": {
        "nbb": "0.2.8",
        "@google-cloud/functions-framework": "~1.9.0"
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

#js {:hello hello}
```

Then deploy the function with:

```
$ gcloud functions deploy hello --runtime nodejs14 --trigger-http
```

Also see [Nbb on AWS Lambda](aws_lambda.md).
