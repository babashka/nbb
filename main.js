const tbd = require('./out/tbd.core')
const fs = require('fs')

const source = fs.readFileSync(process.argv[process.argv.length - 1]).toString()
tbd.eval_code(source)
