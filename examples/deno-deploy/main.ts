// JS/TS wrapper: the ESM entrypoint Deno Deploy (and `deno serve`) sees.
// It boots nbb, evaluates handler.cljs, and re-exports the returned
// { fetch } object as this module's default export.
import { loadFile, addClassPath } from "npm:nbb";

addClassPath("."); // needed to require sibling .cljs files

const app = await loadFile("./handler.cljs");

export default { fetch: app.fetch };
