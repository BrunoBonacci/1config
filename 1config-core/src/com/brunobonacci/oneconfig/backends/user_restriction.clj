(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.user-restriction
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [amazonica.aws.securitytoken :as sts]
            [com.brunobonacci.oneconfig.profiles :as prof]
            [com.brunobonacci.oneconfig.util :refer [safely]]))


;;
;; ```
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; user-restriction-backend
;;  | If the user has defined some restriction, this backend
;;  | will ensure that the entry conforms to the restrictions
;;  | defined by the user.
;; ```
;;

(defn- aws-account-id
  []
  (safely
   (-> (sts/get-caller-identity {}) :account)
   :on-error
   :log-errors false
   :default "local"))


(defn- check-user-restrictions
  [config-entry]
  (prof/apply-restrictions! (prof/user-profiles) (aws-account-id) config-entry))



(deftype UserRestrictionBackend
    [store]

  IConfigClient

  (find [_ config-entry]
    (find store config-entry))


  IConfigBackend

  (load [_ config-entry]
    (load store config-entry))


  (save [_ config-entry]
    (check-user-restrictions config-entry)
    (save store config-entry))


  (list [_ filters]
    (list store filters)))


(defn user-restriction-backend
  [store]
  (when store
    (UserRestrictionBackend. store)))
