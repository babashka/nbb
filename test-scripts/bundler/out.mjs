import { loadFile, registerModule } from 'nbb'
import * as term_size from 'term-size'
registerModule(term_size, 'term-size')
import * as chalk from 'chalk'
registerModule(chalk, 'chalk')
import 'nbb/lib/nbb_promesa.js'
