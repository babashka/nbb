(require '[nbb.repl :as repl])

(repl/repl
  {:init #(apply require '[[promesa.core :as p]])})
