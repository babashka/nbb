(ns example
  (:require
   ["vscode-extension-tester" :refer [VSBrowser WebDriver
                                      ActivityBar, ContextMenu, EditorView, TitleBar
                                      ExTester]]
   [promesa.core :as p]))

(defn sleep [ms]
  (js/Promise. (fn [resolve]
                 (js/setTimeout resolve ms))))

(def the-browser (atom nil))

;; make sure to export PATH=test-resources:$PATH to load correct chromedriver

(-> (p/let [tester (ExTester.)
            ;; _ (.setupRequirements #js {:vscodeVersion "1.60.2"})
            _ (.downloadCode tester "1.60.2")
            _ (.downloadChromeDriver tester "1.60.2")
            browser (VSBrowser. "1.60.2" "stable" #js {} "info")
            browser (.start browser "test-resources/Visual Studio Code.app/Contents/MacOS/Electron")
            _ (reset! the-browser browser)
            _ (sleep 1000)
            _ (.installVsix tester #js {:vsixFile "/Users/borkdude/Downloads/calva-2.0.214-1001-bb-jack-in-8340d2a5.vsix"})]
      (.quit browser))
    (.catch (fn [err]
              (prn err)
              (when-let [b @the-browser]
                (.quit b)))))
