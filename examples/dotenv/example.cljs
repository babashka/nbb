(ns dotenv.example
  (:require ["dotenv/config"]))

(prn js/process.env.DATABASE_URL)
