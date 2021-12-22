(ns nbb.impl.shadow-hooks)

(defn hook
  {:shadow.build/stage :flush}
  [build-state & _args]
  build-state)
