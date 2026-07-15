// JS/TS wrapper: the ESM entrypoint Deno Deploy (and `deno serve`) sees.
// It boots nbb, evaluates handler.cljs, and re-exports the returned
// { fetch } object as this module's default export.
//
// Dependencies are imported here at the top level so Deno resolves them
// natively, then handed to nbb with registerModule. If handler.cljs
// required "jsr:@hono/hono" directly, nbb's runtime import would run in
// Node-compat context and fail with ERR_UNSUPPORTED_ESM_URL_SCHEME.
// TODO: nbb is also published on jsr as @babashka/nbb. Its jsr exports map
// only exposes the CLI ("." -> cli.js), not the loadFile/registerModule API,
// so we import the API from npm here. If the jsr package exported the API too,
// we could `import { loadFile } from "jsr:@babashka/nbb"` and nbb would run in
// Deno-native context -- then handler.cljs could require "jsr:@hono/hono"
// directly and the registerModule dance below would be unnecessary.
import { loadFile, registerModule, addClassPath } from "npm:nbb@1.5.211";
import * as hono from "jsr:@hono/hono";

registerModule(hono, "@hono/hono");
addClassPath("."); // needed to require sibling .cljs files

const app = await loadFile("./handler.cljs");

export default { fetch: app.fetch };
