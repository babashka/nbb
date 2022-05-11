# Nbb on AWS Lambda

## Creating an nbb lambda

All you need to do to get nbb running on AWS Lambda is the following:

package.json:
``` json
{"dependencies": {"nbb": "0.2.8"}}
```

index.mjs:
``` javascript
import { loadFile } from 'nbb';

const { handler } = await loadFile('./example.cljs');

export { handler }
```

example.cljs:
``` clojure
(ns example)

(defn handler [event _ctx]
  (js/console.log event)
  (js/Promise.resolve #js{:hello "world"}))

#js {:handler handler}
```

Make sure to run `npm install`.

Zip the directory: `zip -r app.zip .`

Go to the AWS Console. `Choose Lambda` -> `Author from Scratch` -> `Runtime Node.s 14.x + arm64`.
The default 128 MB should be sufficient for fast response
times after cold start, but for fast cold starts, higher memory (which comes
with higher CPU) is better.

Then choose `Upload from` and choose the zip file.

You can test the lambda function by creating a test event and invoking it.

To be able to invoke the function via HTTP, you'll first have to `Publish` it.

The API Gateway response has to be a little different so adjust your handler code like this...

example.cljs:
```clojure
(ns example)
(defn handler [event _ctx]
      (js/console.log event)
      (js/Promise.resolve
        (clj->js {:statusCode 200
                  :body       (js/JSON.stringify #js{:hello "world"})})))
#js {:handler handler}
```

After uploading the updated code, under `Configuration > Trigger` you can add an
API Gateway trigger. Create one and choose `HTTP API` and `Security Open` (make
sure you change this when it becomes a private production lambda rather than
just for the sake of trying nbb on lambda!).

After that you should end up with a public URL like
`https://9fov8nrv4f.execute-api.eu-central-1.amazonaws.com/default/...` which
you can then call from `curl` or via a browser. The response times I got after
the cold start were around 100ms.

As a nice bonus, you can edit the CLJS code [directly in the console](https://twitter.com/borkdude/status/1479786184557617160).

Also check out these resources:

- [nbb serverless example](https://github.com/vharmain/nbb-serverless-example)
by Valtteri Harmainen
- [Serverless site analytics with Clojure nbb and AWS](https://www.loop-code-recur.io/simple-site-analytics-with-serverless-clojure/) by Cyprien Pannier
- [Nbb on Google Cloud Function](gcloud_functions.md)
