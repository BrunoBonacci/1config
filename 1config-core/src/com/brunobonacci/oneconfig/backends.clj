(ns com.brunobonacci.oneconfig.backends
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends
             [dynamo         :refer [dynamo-config-backend default-dynamo-config]]
             [encoding       :refer [make-encoding-wrapper]]
             [file           :refer [filesystem-config-backend]]
             [file1          :refer [file1-config-backend]]
             [hierarchical   :refer [hierarchical-backend]]
             [iam-user       :refer [iam-user-backend]]
             [immutable      :refer [immutable-backend]]
             [kms-encryption :refer [kms-encryption-backend]]
             [logging        :refer [logging-backend]]
             [validation     :refer [validation-backend]]]
            [com.brunobonacci.oneconfig.util :refer :all]))



(def common-wrappers
  (comp make-encoding-wrapper
     validation-backend
     immutable-backend))



(defmulti backend-factory :type)



(defmethod backend-factory :default
  [{:keys [type] :as c}]
  (let [type    (when-not (= :default type) type)
        backend (keyword (or type (default-backend-name)))]
    (when-not (#{:hierarchical :dynamo :fix :fs :filesystem} backend)
      (throw (ex-info "Invalid backend selection" {:backend backend})))

    (backend-factory
     (assoc c :type backend))))



(defmethod backend-factory :hierarchical
  [_]
  (common-wrappers
   (or
    ;; search exclusive configuration in files first
    (some-> (configuration-file-search) file1-config-backend)
    ;; otherwise search in fs and then in dynamo
    (let [kms+dynamo (iam-user-backend
                      (kms-encryption-backend
                       (dynamo-config-backend
                        (default-dynamo-config))))]
      (hierarchical-backend
       [(filesystem-config-backend)
        kms+dynamo]
       [kms+dynamo])))))



(defmethod backend-factory :dynamo
  [_]
  (common-wrappers
   (iam-user-backend
    (kms-encryption-backend
     (dynamo-config-backend
      (default-dynamo-config))))))



(defmethod backend-factory :fix
  [_]
  (->> (configuration-file-search)
     file1-config-backend
     common-wrappers))



(defmethod backend-factory :fs
  [c]
  (backend-factory (assoc c :type :filesystem)))



(defmethod backend-factory :filesystem
  [cfg]
  (->> (filesystem-config-backend cfg)
     common-wrappers))
