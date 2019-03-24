(ns com.brunobonacci.oneconfig
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends
             [dynamo         :refer [dynamo-config-backend default-dynamo-config]]
             [encoding       :refer [make-encoding-wrapper]]
             [file           :refer [readonly-file-config-backend]]
             [file1          :refer [file1-config-backend]]
             [hierarchical   :refer [hierarchical-backend]]
             [immutable      :refer [immutable-backend]]
             [kms-encryption :refer [kms-encryption-backend]]
             [validation     :refer [validation-backend]]]
            [com.brunobonacci.oneconfig.util :refer :all]))



(defn one-config
  "searches in a number of location for the configure file,
   if isn't found in any of the locations it searches into
   dynamodb using the aws roles to access the table.
   It returns a configuration backend if found or nil"
  []
  (make-encoding-wrapper
   (or
    ;; search exclusive configuration in files first
    (some-> (configuration-file-search) file1-config-backend)
    ;; otherwise search in dynamo
    (let [kms+dynamo (kms-encryption-backend
                      (dynamo-config-backend
                       (default-dynamo-config)))]
      (validation-backend
       (immutable-backend
        (hierarchical-backend
         [(readonly-file-config-backend)
          kms+dynamo]
         [kms+dynamo])))))))



(defn configure
  "Returns a configuration entry if found in any
   of the available configuration backends or nil
   if not found"
  ([{:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (configure (one-config) config-entry))
  ([^com.brunobonacci.oneconfig.backend.IConfigBackend config-backend
    {:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (log-configure-request config-entry
    (when config-backend
      (find config-backend config-entry)))))


;; (configure {:key "system1" :env "dev" :version "6.2.4"})
