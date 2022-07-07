import { loadFile, loadString, registerModule } from 'nbb'
import * as term_size from 'term-size'
registerModule(term_size, 'term-size')
import * as chalk from 'chalk'
registerModule(chalk, 'chalk')
import 'nbb/lib/./nbb_cljs_bean.js'
import 'nbb/lib/./nbb_promesa.js'
await loadString("(ns utils)\n\n(prn :utils)\n\n(defn util-fn []\n  (+ 1 2 3))\n")
await loadString("(ns another-namespace\n  (:require\n   [\"term-size$default\" :as term-size]\n   [cljs-bean.core :as bean]\n   [utils :as u]))\n\n(defn cool-fn []\n  [(u/util-fn) (bean/bean (term-size))])\n")
await loadString("(ns example\n  (:require\n   [\"chalk$default\" :as chalk]\n   [another-namespace :as another]\n   [promesa.core :as p]\n   [utils :as u]))\n\n(def log js/console.log)\n\n(log (chalk/blue \"hello\"))\n(prn (another/cool-fn))\n\n(p/-> (p/delay 1000 (u/util-fn))\n      prn)\n")
