(ns print-cli-args.core)

(defn -main [& args]
  (println "Your command line arguments:"
           (or args
               "None")))
