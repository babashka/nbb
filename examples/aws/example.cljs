(ns example
  (:require ["@aws-sdk/client-s3" :refer [S3Client ListBucketsCommand ]]
            [promesa.core :as p]))

(p/let [client (new S3Client #js {:region "eu-west-1"})
        response (.send client (new ListBucketsCommand #js {}))
        {:keys [Buckets]} (js->clj response :keywordize-keys true)]
  (prn Buckets))
