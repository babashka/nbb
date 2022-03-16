(ns example
  (:require ["node-ical$default.sync" :as ical]
            [cljs.pprint :refer [pprint]]))

(def direct-events (js->clj (ical/parseICS "
BEGIN:VCALENDAR
VERSION:2.0
CALSCALE:GREGORIAN
BEGIN:VEVENT
SUMMARY:Hey look! An example event!
DTSTART;TZID=America/New_York:20130802T103400
DTEND;TZID=America/New_York:20130802T110400
LOCATION:1000 Broadway Ave.\\, Brooklyn
DESCRIPTION: Do something in NY.
STATUS:CONFIRMED
UID:7014-1567468800-1567555199@peterbraden@peterbraden.co.uk
END:VEVENT
END:VCALENDAR")))

(pprint direct-events)
#_
{"7014-1567468800-1567555199@peterbraden@peterbraden.co.uk"
 {"params" [],
  "uid" "7014-1567468800-1567555199@peterbraden@peterbraden.co.uk",
  "datetype" "date-time",
  "summary" "Hey look! An example event!",
  "location" "1000 Broadway Ave., Brooklyn",
  "status" "CONFIRMED",
  "start" #inst "2013-08-02T14:34:00.000-00:00",
  "type" "VEVENT",
  "end" #inst "2013-08-02T15:04:00.000-00:00",
  "description" " Do something in NY."}}
