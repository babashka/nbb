(ns nbb.error
  {:no-doc true}
  (:refer-clojure :exclude [println])
  (:require [clojure.string :as str]
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

(defn error-context [ex src-map]
  (let [{:keys [:file :line :column]} (ex-data ex)]
    (when (and file line)
      (when-let [content (get src-map file)]
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
                                       (str (.padStart (str line-number) max-size "0") "  " line))
                                     (str (str/join (repeat (+ 2 max-size) " ")) line)))
                                 snippet-lines)]
          (str "\n" (str/join "\n" snippet-lines)))))))

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

(defn error-handler [e src-map]
  (let [d (ex-data e)
        sci-error? (isa? (:type d) :sci/error)
        stacktrace (sci/stacktrace e)]
    (ruler "Nbb error")
    (when-let [name (.-name e)]
      (when-not (= "Error" name)
        (println "Type:    " name)))
    (when-let [m (.-message e)]
      (println (str "Message:  " m)))
    (when-let [d (ex-data (ex-cause e) #_(.getCause e))]
      (print (str "Data:     "))
      (prn d))
    (let [{:keys [:file :line :column]} d]
      (when line
        (println (str "Location: "
                      (when file (str file ":"))
                      line ":" column""))))
    (when-let [phase (:phase d)]
      (println "Phase:   " phase))
    (when-let [ec (when sci-error?
                      (error-context e src-map))]
        (ruler "Context")
        (println ec))
    (when-let [locals (not-empty (:locals d))]
      (ruler "Locals")
      (print-locals locals))
    (when sci-error?
      (when-let
          [st (let [st (with-out-str
                         (when stacktrace
                           (print-stacktrace stacktrace src-map)))]
                (when-not (str/blank? st) st))]
        (println)
        (ruler "Stack trace")
        (println st)))))
