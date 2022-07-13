(ns nbb.impl.bencode
  "Bencode support, taken from https://github.com/djblue/nrepl-cljs/blob/master/src/nrepl/bencode.cljs")

(defn- index-of [s c]
  (let [i (.indexOf s c)]
    (if (< i 0) (throw (js/Error. "out of input")) i)))

(defn- slice
  ([buffer start]
   (if (< (.-length buffer) start)
     (throw (js/Error. "out of input"))
     (.slice buffer start)))
  ([buffer start end]
   (if (> end (.-length buffer))
     (throw (js/Error. "out of input"))
     (.slice buffer start end))))

(defn- decode-recur [data opts]
  (case (str (slice data 0 1))
    "i"
    (let [data (slice data 1)
          i (index-of data "e")]
      [(js/parseInt (slice data 0 i))
       (slice data (inc i))])
    "l"
    (let [data (slice data 1)]
      (loop [data data v (transient [])]
        (if (= (str (slice data 0 1)) "e")
          [(persistent! v) (slice data 1)]
          (let [[value data] (decode-recur data opts)]
            (recur data (conj! v value))))))
    "d"
    (let [data (slice data 1)
          {:keys [keywordize-keys]} opts]
      (loop [data data m (transient {})]
        (if (= (str (slice data 0 1)) "e")
          [(persistent! m) (slice data 1)]
          (let [[k data] (decode-recur data opts)
                [v data] (decode-recur data opts)
                k (if keywordize-keys (keyword k) k)]
            (recur data (assoc! m k v))))))
    (let [i (index-of data ":")
          n (js/parseInt (slice data 0 i))
          data (slice data (inc i))]
      [(str (slice data 0 n)) (slice data n)])))

(defn decode [data & opts]
  (try
    (decode-recur data opts)
    (catch js/Error _e [nil data])))

(defn decode-all [data & opts]
  (loop [items [] data data]
    (let [[item data] (apply decode data opts)]
      (if (nil? item)
        [items data]
        (recur (conj items item) data)))))

(defn read-bencode [string] (first (decode string)))

(defn utf8-bytes [s]
  (.-length (js/Buffer.from s)))

(defn encode [data]
  (cond
    (string? data)
    (str (utf8-bytes data) ":" data)
    (or (keyword? data)
        (symbol? data))
    (recur (str
            (when-let [n (namespace data)]
              (str n "/"))
            (name data)))
    (number? data)
    (str "i" data "e")
    (or (set? data) (vector? data) (nil? data))
    (str "l" (apply str (map encode data)) "e")
    (map? data)
    (str "d" (->> data
                  (sort-by first)
                  (map (fn [[k v]]
                         (str (encode k) (encode v))))
                  (apply str))
         "e")))

(defn write-bencode [data]
  (encode data))
