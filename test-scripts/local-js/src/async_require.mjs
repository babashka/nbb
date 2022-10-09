import { loadString } from '../../../index.mjs';

var res1 = loadString("(ns foo) (str (ns-name *ns*))");
var res2 = loadString("(ns bar) (str (ns-name *ns*))");
var res3 = loadString("(ns baz) (str (ns-name *ns*))");
var res4 = loadString("(str (ns-name *ns*))");

res1 = await res1;
res2 = await res2;
res3 = await res3;
res4 = await res4;

// console.log(res1, res2, res3, res4);

export { res1, res2, res3, res4 };
