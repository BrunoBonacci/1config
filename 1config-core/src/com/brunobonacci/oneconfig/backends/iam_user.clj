(ns com.brunobonacci.oneconfig.backends.iam-user
  (:refer-clojure :exclude [find load list])
  (:require [amazonica.aws.identitymanagement :as iam]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [safely.core :refer [safely]]))

(deftype IamUserEnrichmentBackend [store]

  IConfigClient

  (find [_ {:keys [key env version change-num] :as config-entry}]
    (find store config-entry))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (load store config-entry))


  (save [_ config-entry]
    ;; read-user from IAM
    (->> (safely
           [[:user (-> (iam/get-user) :user :arn)]]
           :on-error
           :default [])
       (into config-entry)
       (save store)))

  (list [_ filters]
    (list store filters)))



(defn iam-user-backend
  [store]
  (when store
    (IamUserEnrichmentBackend. store)))
