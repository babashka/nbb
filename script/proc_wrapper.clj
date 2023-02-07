(ns proc-wrapper
  (:require [babashka.process :as process :refer [process]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def output-lock (Object.))

(defn output-wrapper [stream prefix writer]
  (let [buf (byte-array 1024)
        buffer (atom nil)]
    (loop []
      (let [read (.read stream buf)]
        (when-not (= -1 read)
          (let [s (String. buf 0 read)
                last-newline (str/last-index-of s \newline)
                before-last-newline (when last-newline (subs s 0 last-newline))
                after-last-newline (if last-newline
                                     (subs s (inc last-newline))
                                     s)]
            (when before-last-newline
              (let [buffered @buffer
                    _ (reset! buffer nil)
                    lines (str/split-lines (str buffered before-last-newline))]
                (doseq [l lines]
                  (locking output-lock
                    (binding [*out* writer]
                      (println prefix l))))))
            ;; (Thread/sleep (rand-int 100))
            (swap! buffer (fn [buffer]
                            (if after-last-newline
                              (str buffer after-last-newline)
                              buffer)))
            (recur)))))))

#_(defn output-wrapper [stream prefix writer]
  (let [rdr (io/reader stream)
        lines (line-seq rdr)]
    (run! #(binding [*out* writer]
             (println prefix %)) lines)))

(defn wrap [prefix args]
  (let [proc (apply process args)
        output-out (future (output-wrapper (:out proc) prefix *out*))
        output-err (future (output-wrapper (:err proc) prefix *err*))
        checked (process/check proc)]
    @output-out
    @output-err
    checked))
