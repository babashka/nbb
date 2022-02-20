(ns print-cli-args.core
  (:require [clojure.string :as str]))

(def cmd-line-args (not-empty (js->clj (.slice js/process.argv 2))))

(println "Your command line arguments:"
         (or (some->> cmd-line-args (str/join " "))
             "None"))
