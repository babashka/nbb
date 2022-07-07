
import { loadFile, registerModule } from '../../lib/nbb_api.js'
import * as nbb_dot_internal_dot_chalk from 'chalk'
registerModule(nbb_dot_internal_dot_chalk, 'chalk')
await loadFile('example.cljs')