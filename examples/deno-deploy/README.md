# Nbb on Deno Deploy

An HTTP handler written in ClojureScript, run by nbb on
[Deno Deploy](https://deno.com/deploy).

## Files

- `handler.cljs`: builds a [Hono](https://hono.dev/) router, returns `#js {:fetch ...}`.
- `main.ts`: ESM entrypoint. Boots nbb, evaluates `handler.cljs`, re-exports its `fetch`.
- `deno.json`: `dev` task.

## Run locally

``` shell
deno task dev
```

``` shell
curl http://localhost:8000/
curl http://localhost:8000/hello/borkdude
```

## Deploy

Create an app on [Deno Deploy](https://app.deno.com) from a repository that
contains this directory. Set:

- App directory: `examples/deno-deploy`
- Entrypoint: `main.ts`
- Install and build commands: empty

nbb comes from npm and Hono from jsr, so there is no install step.

See also [Nbb on AWS Lambda](../../doc/aws_lambda.md).
