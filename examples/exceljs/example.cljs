(ns example
  (:require ["exceljs$default" :refer [Workbook]]
            [promesa.core :as p]))

(def workbook (Workbook.))

(p/let [_ (-> workbook
            (.-xlsx)
            (.readFile "sample.xlsx"))]
  (prn (-> workbook
           (.getWorksheet "Sheet1")
           (.getCell "A1")
           (.-value))))

