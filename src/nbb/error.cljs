(ns nbb.error
  {:no-doc true}
  (:refer-clojure :exclude [println])
  (:require
   ["fs" :as fs]
   [clojure.string :as str]
   [sci.core :as sci]))

(defn println [& strs]
  (.error js/console (str/join " " strs)))

(defn ruler [title]
  (println (apply str "----- " title " " (repeat (- 50 7 (count title)) \-))))

(defn split-stacktrace [stacktrace verbose?]
  (if verbose? [stacktrace]
      (let [stack-count (count stacktrace)]
        (if (<= stack-count 10)
          [stacktrace]
          [(take 5 stacktrace)
           (drop (- stack-count 5) stacktrace)]))))

(defn print-stacktrace
  [stacktrace {:keys [:verbose?]}]
  (let [stacktrace (sci/format-stacktrace stacktrace)
        segments (split-stacktrace stacktrace verbose?)
        [fst snd] segments]
    (run! #(print % "\n") fst)
    (when snd
      (print "...\n")
      (run! #(print % "\n") snd))))

(defn read-maybe-file [file]
  (when (fs/existsSync file)
    (str (fs/readFileSync file))))

(defn error-context [ex]
  (let [{:keys [:file :line :column]} (ex-data ex)]
    (when (and file line)
      (when-let [content (read-maybe-file file)]
        (let [matching-line (dec line)
              start-line (max (- matching-line 4) 0)
              end-line (+ matching-line 6)
              [before after] (->>
                              (str/split-lines content)
                              (map-indexed list)
                              (drop start-line)
                              (take (- end-line start-line))
                              (split-at (inc (- matching-line start-line))))
              snippet-lines (concat before
                                    [[nil (str (str/join "" (repeat (dec column) " "))
                                               (str "^--- " (ex-message ex)))]]
                                    after)
              indices (map first snippet-lines)
              max-size (reduce max 0 (map (comp count str) indices))
              snippet-lines (map (fn [[idx line]]
                                   (if idx
                                     (let [line-number (inc idx)]
                                       (str (.padStart (str line-number ":") max-size "0") " " line))
                                     (str (str/join (repeat (+ 2 max-size) " ")) line)))
                                 snippet-lines)]
          (str/join "\n" snippet-lines))))))

(defn right-pad [s n]
  (let [n (- n (count s))]
    (str s (str/join (repeat n " ")))))

(defn print-locals [locals]
  (let [max-name-length (reduce max 0 (map (comp count str)
                                           (keys locals)))
        max-name-length (+ max-name-length 2)]
    (println
     (with-out-str (binding [*print-length* 10
                             *print-level* 2]
                     (doseq [[k v] locals]
                       (print (str (right-pad (str k ": ") max-name-length)))
                       ;; print nil as nil
                       (prn v)))))))

(defn error-handler [e opts]
  (let [stacktrace (sci/stacktrace e)
        d (ex-data e)
        sci-error? (isa? (:type d) :sci/error)]
    (ruler "Error")
    (when-let [name (.-name e)]
      (when-not (= "Error" name)
        (println "Type:    " name)))
    (when-let [m (.-message e)]
      (println (str "Message:  " m)))
    (when (:debug opts)
      (when-let [d (ex-data (ex-cause e) #_(.getCause e))]
        (print (str "Data:     "))
        (prn d)))
    (let [{:keys [:file :line :column]} d]
      (when line
        (println (str "Location: "
                      (when file (str file ":"))
                      line ":" column""))))
    (when-let [phase (:phase d)]
      (println "Phase:   " phase))
    (when-let [ec (when sci-error?
                    (error-context e))]
      (println)
      (ruler "Context")
      (println ec))
    (when (:debug opts)
      (when-let [locals (not-empty (:locals d))]
        (ruler "Locals")
        (print-locals locals)))
    (when sci-error?
      (when-let
          [st (let [st (with-out-str
                         (when stacktrace
                           (print-stacktrace stacktrace nil #_src-map)))]
                (when-not (str/blank? st) st))]
        (println)
        (ruler "Stack trace")
        (println st)))))
