import { loadString } from '../../index.mjs';

const res1 = loadString("(ns foo) (str (ns-name *ns*))");
const res2 = loadString("(ns bar) (str (ns-name *ns*))");
const res3 = loadString("(ns bar) (str (ns-name *ns*))");
const res4 = loadString("(str (ns-name *ns*))");

await res1;
await res2;
await res3;
await res4;

export { res1, res2, res3, res4 };
