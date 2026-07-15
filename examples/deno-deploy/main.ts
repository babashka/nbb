// ESM entrypoint for Deno Deploy and `deno serve`.
// @hono/hono is imported natively here, then handed to nbb with
// registerModule. Requiring "jsr:@hono/hono" from handler.cljs instead fails
// on Deploy: nbb loaded from npm runs in Node-compat context and its runtime
// import rejects the jsr: scheme.
// TODO: nbb's jsr package (@babashka/nbb) exports only the CLI, not the API.
// If it exported loadFile/registerModule, importing nbb from jsr would let
// handler.cljs require "jsr:@hono/hono" directly.
import { loadFile, registerModule, addClassPath } from "npm:nbb@1.5.211";
import * as hono from "jsr:@hono/hono";

registerModule(hono, "@hono/hono");
addClassPath("."); // require sibling .cljs files

const app = await loadFile("./handler.cljs");

export default { fetch: app.fetch };
