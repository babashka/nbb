import { loadString } from './index.mjs';


const res0 = loadString("(str (ns-name *ns*))");

const res1 = loadString("(ns foo) (str (ns-name *ns*))");

const res2 = loadString("(ns bar) (str (ns-name *ns*))");

const res3 = loadString("(str (ns-name *ns*))");

console.log(await res0); // user
console.log(await res2); // bar
console.log(await res1);  // foo
console.log(await res3); // user
