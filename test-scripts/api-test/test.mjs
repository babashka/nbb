import { loadFile, loadString } from 'nbb';

const result = await loadFile('test.cljs');

console.log(result);

const add = await loadString('(fn [x y] (+ x y))');

console.log(add(1,2));
