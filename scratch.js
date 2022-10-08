import { loadString } from './index.mjs';

const res2 = await loadString("(ns bar) (str (ns-name *ns*))");

const res3 = await loadString("(str (ns-name *ns*))");

console.log(await res2); // bar
console.log(await res3); // bar, but should be user
