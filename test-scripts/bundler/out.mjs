
import { loadFile, registerModule } from '../../lib/nbb_api.js'

import '../../lib/nbb_promesa.js'

import * as nbb_dot_internal_dot_chalk from 'chalk'
registerModule(nbb_dot_internal_dot_chalk, 'chalk')
import * as foo from 'term-size'
registerModule(foo, 'term-size')
