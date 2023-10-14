(ns fnbb.config)

(def config {:host (or js/process.env.HOST "127.0.0.1")
             :port (or js/process.env.PORT 3000)
             :logger true,
             :ignoreTrailingSlash true
             :cookie-secret "cookie-s3cr3t"
             :jwt-secret "jwt-s3cr3t"})
