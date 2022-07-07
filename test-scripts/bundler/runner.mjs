import { addClassPath, loadFile } from '../../lib/nbb_api.js'

console.log(typeof(process.cwd()));

addClassPath(process.cwd());

import './out.mjs'

await loadFile('example.cljs')
