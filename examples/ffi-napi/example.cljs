(ns foo (:require ["ffi-napi" :as ffi]))

(def lib (ffi/Library "libncurses" #js {"initscr" #js ["void" #js []]
                                        "printw" #js ["int" #js ["string"]]
                                        "refresh" #js ["void" #js []]
                                        "getchar" #js ["char" #js []]
                                        "endwin" #js ["void" #js []]}))

(def initscr (.-initscr lib))
(def printw (.-printw lib))
(def refresh (.-refresh lib))
(def getchar (.-getchar lib))
(def endwin (.-endwin lib))

(initscr)
(printw "Hello world!\n")
(refresh)
(getchar)
(endwin)
