(ns script
  (:require ["dayjs$default" :as dayjs]
            ["moment$default" :as moment]))

;; reloading ns form should work
(ns script
  (:require ["dayjs$default" :as dayjs]
            ["moment$default" :as moment]))

(prn (dayjs))
(prn (moment))
