(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.iam-user
  (:refer-clojure :exclude [find load list])
  (:require [amazonica.aws.securitytoken :as sts]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer [safely]]))

;;
;;```
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; iam-user-backend
;;  | It retrieves the caller-identity via the AWS STS apis and injects it into
;;  | the config entry so that it is tracked which user performed the action
;;  |
;;  | + {:user (sts/get-caller-identity)}
;;```
;;


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
           [[:user (-> (sts/get-caller-identity {}) :arn)]]
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
