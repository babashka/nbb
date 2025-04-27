(ns example
  (:require ["node:fs" :as fs]
            [promesa.core :as p]))

;; example derived from https://www.tutorialspoint.com/webassembly/webassembly_examples.htm

;; The following C code is compiled to wasm:

;; void displaylog(int n);
;; /* function returning the max between two numbers */
;; int max(int num1, int num2) {
;;    /* local variable declaration */ int result;
;;    if (num1 > num2)
;;       result = num1;
;;    else result = num2;
;;       displaylog(result);
;;    return result;
;; }

;; You can do that yourself in https://wasdk.github.io/WasmFiddle/
;; And then we load the exported .wasm file here:

(def wasm-buffer (fs/readFileSync "program.wasm"))

(def import-obj #js {:env #js {:displaylog js/console.log}})

(p/let [max (p/-> (.instantiate js/WebAssembly wasm-buffer import-obj)
                  (.. -instance -exports -max))]
  (max 1 2))
