(ns com.brunobonacci.oneconfig
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer :all]
            [com.brunobonacci.oneconfig.backends.file
             :refer [readonly-file-config-backend]]
            [com.brunobonacci.oneconfig.backends.encoding
             :refer [make-encoding-wrapper]]
            [com.brunobonacci.oneconfig.backends.dynamo
             :refer [dynamo-config-backend default-dynamo-config]]))



(defn retrieve-config
  "searches in a number of location for the configure file,
   if isn't found in any of the locations it searches into
   dynamodb using the aws roles to access the table.
   It returns a configuration backend if found or nil"
  []
  (make-encoding-wrapper
   (or
    ;; search configuration in files first
    (some-> (configuration-file-search) readonly-file-config-backend)
    ;; otherwise search in dynamo
    (dynamo-config-backend (default-dynamo-config)))))



(defn configure
  "Returns a configuration entry if found in any
   of the available configuration backends or nil
   if not found"
  ([{:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (configure (retrieve-config) config-entry))
  ([^com.brunobonacci.oneconfig.backend.IConfigBackend config-backend
    {:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (when config-backend
     (find config-backend config-entry))))


;; (configure {:key "system1" :env "dev" :version "6.2.4"})
