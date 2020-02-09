(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends
             [dynamo           :refer [dynamo-config-backend default-dynamo-config]]
             [encoding         :refer [make-encoding-wrapper]]
             [file             :refer [filesystem-config-backend]]
             [file1            :refer [file1-config-backend]]
             [hierarchical     :refer [hierarchical-backend]]
             [iam-user         :refer [iam-user-backend]]
             [immutable        :refer [immutable-backend]]
             [kms-encryption   :refer [kms-encryption-backend]]
             [logging          :refer [logging-backend]]
             [user-restriction :refer [user-restriction-backend]]
             [validation       :refer [validation-backend]]]
            [com.brunobonacci.oneconfig.util :refer :all]))


;;
;; The different backends work as layers with different responsibilities.
;; Each layer is responsible to ensure one aspect only, such as the
;; configuration is a valid json or that it encrypted before it reaches
;; a shared storage.
;;
;; They all work based on the IConfigBackend protocol and they might
;; accept a secondary backed where they will "pass-on" the request
;; after they are done with what they need to do.
;;
;; Here we will explore what each backend does and how the load/store
;; operation flow through the various backend layers
;;
;; ```
;; SET `{:key "service1", :env "dev", :version "1.2.3",
;;  |    :value "{:foo 1}", :content-type "edn"}
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; immutable-backend
;;  | it ensures that a new `change-num` is set so that if it is present in
;;  | the user request it will be overwritten with a value given by the system.
;;  | This ensures that if a config entry if returned from a GET operation,
;;  | updated and the SET (stored) a new config-entry will be created.
;;  |
;;  | + {:change-num (new-num)}
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; validation-backend
;;  | it ensures that the request (config-entry) has all the necessary
;;  | information and that it is in the expected type/format
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; user-restriction-backend
;;  | If the user has defined some restriction, this backend
;;  | will ensure that the entry conforms to the restrictions
;;  | defined by the user.
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; encoding-backend
;;  | it ensures that the `:value` is in the format described by the
;;  | `:content-type`, if-not it encodes it accordingly.
;;  | For example a json value could be passed as a nested map of values
;;  | in such case it will call the appropriate encoder and render a
;;  | the value in the correct format.
;;  | Most of the encoding will return a String representation of the value,
;;  | however the string itself is a value value for edn for example.
;;  | So in order to disambiguate the two cases it looks for an additional
;;  | key callled `:encoded` which can be `true|false` and tells whether
;;  | the given input value is already encoded or not.
;;  | If it is already encoded then the values is decoded to ensure that
;;  | it is valid and avoid malformed input and syntax errors and it is
;;  | finally stored *as it is* which guarantee that comments and formatting
;;  | are preserved.
;;  |
;;  | + {:value (mashall-value value)}
;;  | - {:encoded true|false}
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; iam-user-backend
;;  | It retrieves the caller-identity via the AWS STS apis and injects it into
;;  | the config entry so that it is tracked which user performed the action
;;  |
;;  | + {:user (sts/get-caller-identity)}
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; kms-encryption-backend
;;  | It expects a encoded value as a String and it encrypts the value with
;;  | a generated data keys. Then it uses a master key to encrypt the data key
;;  | together and it stores them together in the given backend store.
;;  | If the master key doesn't exists it will create it.
;;  | additionally it uses a number of values of the config-entry as encryption
;;  | context to ensure that these values won't be tampered by direct editing
;;  | into the database. Such values include:
;;  | `[:key :env :version :change-num :content-type :user]`
;;  | It finally add the encrypted value back into the entry and add which key
;;  | and key-alias was used for the encryption (informative only).
;;  |
;;  | + {:value (encrypt context value)}
;;  | + {:master-key "arn", :master-key-alias "key-alias"}
;;  |
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; dynamo-backend
;;  | it stores the entry into DynamoDB table called `1Config` in the given
;;  | region with a conditional insert which it will fail if an entry
;;  | with the same key exists.
;; -----------------------------------------------------------------------------
;; ```
;;


(def common-wrappers
  (comp make-encoding-wrapper
     validation-backend
     user-restriction-backend
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
