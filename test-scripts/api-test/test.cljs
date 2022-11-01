(require '[medley.core])

(first (keys (medley.core/index-by :id [{:id 1} {:id 2}])))

