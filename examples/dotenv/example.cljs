(ns dotenv.example
  (:require ["dotenv" :as dotenv]))

(dotenv/config)

(prn js/process.env.DATABASE_URL)
