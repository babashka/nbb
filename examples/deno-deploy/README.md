# Nbb on Deno Deploy

An HTTP function written in ClojureScript, run by nbb, served on
[Deno Deploy](https://deno.com/deploy).

## Files

- `handler.cljs` — the app. Builds a [Hono](https://hono.dev/) router and
  returns `#js {:fetch ...}`.
- `main.ts` — the ESM entrypoint Deno Deploy runs. It boots nbb, evaluates
  `handler.cljs`, and re-exports the returned object as the module's default
  export, so `fetch` is where Deploy expects it.
- `deno.json` — a `dev` task for local runs.

## Run locally

``` shell
deno task dev
# or: deno serve -A main.ts
```

Then:

``` shell
curl http://localhost:8000/
curl http://localhost:8000/hello/borkdude
```

## Deploy

Install [`deployctl`](https://docs.deno.com/deploy/manual/deployctl/) and run
from this directory:

``` shell
deployctl deploy --entrypoint main.ts
```

`main.ts` requires nbb from npm and Hono from jsr, so no separate install step
is needed — Deploy resolves them on push.

See also [Nbb on AWS Lambda](../../doc/aws_lambda.md), which uses the same
`loadFile` wrapper pattern.
