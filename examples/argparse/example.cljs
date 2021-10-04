(ns example
  (:require
   ["argparse" :as argparse :refer [ArgumentParser]]))

(def parser (ArgumentParser. #js {:prog "example.cljs"
                                  :description "Example!"}))

(.add_argument parser "-f" "--foo" #js {:help "foo bar"})

(.dir js/console (.parse_args parser (clj->js (vec *command-line-args*))))
