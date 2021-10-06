(ns example
  (:require ["vscode-test" :refer [downloadAndUnzipVSCode]]
            ["vscode-extension-tester" :refer [VSBrowser WebDriver
                                               ActivityBar, ContextMenu, EditorView, TitleBar]]
            [promesa.core :as p]
            ))

;; https://github.com/redhat-developer/vscode-extension-tester/wiki/Writing-Simple-Tests
;; download chromedriver 91 from https://chromedriver.chromium.org/downloads

(p/let [bin (downloadAndUnzipVSCode "1.60.2")
        browser (VSBrowser. "1.60.2" "stable" #js {} "info")
        browser (.start browser (str bin))]
  (prn browser)
  (.quit browser))
