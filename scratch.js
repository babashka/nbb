import { loadString } from './index.mjs';

const res = await loadString("(ns foo) (str (ns-name *ns*))");

console.log(res);

const res2 = await loadString("(str (ns-name *ns*))");

console.log(res2);
