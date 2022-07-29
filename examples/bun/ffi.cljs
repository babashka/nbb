(ns ffi (:require ["bun:ffi" :as ffi ]))

(def symbols (-> (ffi/dlopen (str "libncurses." ffi/suffix)
                             (clj->js {:initscr {:args []
                                                 :returns :void}
                                       :printw {:returns :int
                                                :args [:char*]}
                                       :refresh {:returns :void
                                                 :args []}
                                       :getchar {:returns :char
                                                 :args []}
                                       :endwin {:returns :void
                                                :args []}}))
                 (.-symbols)))

(def initscr (.-initscr symbols))
(def printw (.-printw symbols))
(def refresh (.-refresh symbols))
(def getchar (.-getchar symbols))
(def endwin (.-endwin symbols))

(def my-str "Hello world!\n")
(def data (-> (new js/TextEncoder) (.encode (str my-str "\0"))))

(initscr)
(printw (ffi/ptr data))
(refresh)
(getchar)
(endwin)
