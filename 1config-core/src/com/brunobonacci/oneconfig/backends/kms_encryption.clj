(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
 com.brunobonacci.oneconfig.backends.kms-encryption
  (:refer-clojure :exclude [find load list])
  (:require [clojure.string :as str]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer [clean-map lazy-mapcat env]]
            [com.brunobonacci.oneconfig.aws :as aws]
            [where.core :refer [where]]
            [com.brunobonacci.oneconfig.util :refer [safely]])
  (:import com.amazonaws.encryptionsdk.AwsCrypto
           com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
           com.amazonaws.PredefinedClientConfigurations
           com.amazonaws.auth.DefaultAWSCredentialsProviderChain
           [com.amazonaws.regions Region Regions DefaultAwsRegionProviderChain]
           java.util.Collections))



;;
;;```
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
;;```
;;


(def ^:private aws-region
  (delay
    (or
      ;; check env
      (some-> (or (env "AWS_REGION") (env "AWS_DEFAULT_REGION")) Regions/fromName Region/getRegion)
      ;; use default prodider chain, this call blocks and it is slow on non EC2
      (some-> (safely (.getRegion (new DefaultAwsRegionProviderChain))
                :on-error :default nil :log-errors false)
        Regions/fromName Region/getRegion)
      ;; this call blocks and it is slow on non EC2
      (Regions/getCurrentRegion)
      ;; us-west-2 (??)
      (-> Regions/DEFAULT_REGION Region/getRegion))))



(def arn?
  (where :starts-with? "arn:"))



(defn key-id ^String
  [^String arn-or-key]
  (if (arn? arn-or-key)
    (->> arn-or-key
      (re-find #".*:key/([a-z0-9-]+)")
      second)
    arn-or-key))



(defn- kms-client
  "returns a cached kms client"
  []
  (aws/make-client (aws/default-cfg) :kms))



(def lazy-list-aliases
  (aws/lazy-paginated-query
    #(aws/invoke! % :ListAliases %2)
    :NextMarker :Marker :Aliases))



(defn master-keys
  []
  (->>
    (lazy-list-aliases (kms-client) {})
    (filter (where :AliasName :starts-with? "alias/1Config/"))
    ;; build key ARN out of key alias.
    (map (fn [{:keys [TargetKeyId AliasArn AliasName] :as m}]
           (let [arn
                 (->
                   (re-find (re-pattern (str "(.*):\\Q" AliasName "\\E")) AliasArn)
                   second
                   (str ":key/" TargetKeyId))]
             (assoc m :key-arn arn))))
    (map (juxt :AliasName :key-arn))
    (into {})))



(defn normalize-alias
  [alias]
  (cond
    (nil? alias)                        nil
    (str/starts-with? alias "alias/")   alias
    (str/starts-with? alias "1Config/") (str "alias/" alias)
    :default                            (str "alias/1Config/" alias)))



(defn create-master-key
  [key-name description]
  (let [mk (aws/invoke! (kms-client) :CreateKey
             {:Description description
              :KeyUsage "ENCRYPT_DECRYPT"
              :Origin "AWS_KMS"})]
    (aws/invoke! (kms-client) :CreateAlias
      {:AliasName (normalize-alias key-name)
       :TargetKeyId (-> mk :KeyMetadata :KeyId)})
    (-> mk :KeyMetadata :Arn)))



;;
;; AWS Cryto utility
(defonce ^:private ^AwsCrypto crypto
  (AwsCrypto.))



(defn- encrypt
  ([^String payload master-key-id]
   (encrypt payload master-key-id {}))
  ([^String payload master-key-id context]
   (let [ ;; retrieving the master key to generate the data key
         ^KmsMasterKeyProvider master-key
         (KmsMasterKeyProvider.
           (DefaultAWSCredentialsProviderChain.)
           ^Region @aws-region
           (PredefinedClientConfigurations/defaultConfig)
           (key-id master-key-id))
         ;; sanitize context if present
         ^java.util.Map context (->> (or context {})
                                  (map (fn [[k v]] [(str k) (str v)]))
                                  (into {}))
         ;; the encrypted payload contains an envelop
         ;; with the data key encrypted
         out (.encryptString crypto master-key payload context)]
     {:result      (.getResult out)
      :context     (into {} (.getEncryptionContext out))
      :algorithm   (str (.getCryptoAlgorithm out))
      :master-keys (into [] (.getMasterKeyIds out))})))



(defn- decrypt
  ([^String payload]
   (decrypt payload {}))
  ([^String payload context]
   (let [ ;; retrieving the master key to generate the data key
         ^KmsMasterKeyProvider master-key
         (KmsMasterKeyProvider.
           (DefaultAWSCredentialsProviderChain.)
           ^Region @aws-region
           (PredefinedClientConfigurations/defaultConfig)
           (Collections/emptyList))
         ;; sanitize context if present
         ^java.util.Map context (->> (or context {})
                                  (map (fn [[k v]] [(str k) (str v)]))
                                  (into {}))
         ;; the encrypted payload contains an envelop
         ;; with the data key encrypted
         out (.decryptString crypto master-key payload)
         ;; checking context
         out-ctx (into {} (.getEncryptionContext out))]

     (when (not= context (select-keys out-ctx (keys context)))
       (throw (ex-info "Invalid encryption context. Possible tampering with config-entry."
                {:config-entry context})))
     {:result      (.getResult out)
      :context     out-ctx
      :algorithm   (str (.getCryptoAlgorithm out))
      :master-keys (into [] (.getMasterKeyIds out))})))



(defn arn-or-alias
  [master-key]
  (cond
    (nil? master-key)                        nil
    (str/starts-with? master-key "arn:")     master-key
    :default                                 (normalize-alias master-key)))



(defn- lookup-master-key
  "Lookup a master-key by alias or arn"
  [master-key]
  (try
    (let [master-key (arn-or-alias master-key)
          master-key-arn (-> (aws/invoke! (kms-client) :DescribeKey {:KeyId master-key}) :KeyMetadata :Arn)
          master-key-alias (when-not (= master-key-arn master-key) master-key)]
      (clean-map
        {:master-key master-key-arn
         :master-key-alias master-key-alias}))
    (catch Exception x
      ;; if not found return nil
      (when-not (re-find #" is not found." (.getMessage x))
        (throw x)))))



(defn- resolve-master-key
  "Tries to lookup a key by alias or arn if not exists tries to create one"
  [master-key]
  ;; lookup master key
  (or (lookup-master-key master-key)

    ;; arn keys must already exists
    (when (arn? master-key)
      (throw (ex-info "Cannot find master key" {:master-key master-key})))

    ;; if alias key doesn't exists will create one
    {:master-key-alias (normalize-alias master-key)
     :master-key
     (create-master-key
       (normalize-alias master-key)
       (format "1Config managed key for %s configurations" master-key))}))



(defn- encryption-context
  [config-entry]
  (select-keys config-entry [:key :env :version :change-num :content-type :user]))



(deftype KmsEncryptionConfigBackend [store]

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (when-let [entry (find store config-entry)]
      (let [ctx (encryption-context entry)]
        (update entry :value (comp :result #(decrypt % ctx))))))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (when-let [entry (load store config-entry)]
      (let [ctx (encryption-context entry)]
        (update entry :value (comp :result #(decrypt % ctx))))))


  (save [_ {:keys [key value master-key] :as config-entry}]
    (let [{:keys [master-key-alias master-key]
           }(resolve-master-key (or master-key key))
          context (encryption-context config-entry)
          encrypted (:result
                     (encrypt value master-key context))]
      (as-> config-entry $
        ;; add the encrypted value and the master encryption key
        (assoc $ :value encrypted
          :master-key master-key
          :master-key-alias master-key-alias)
        (clean-map $)
        (save store $))))

  (list [this filters]
    (list store filters)))



(defn kms-encryption-backend
  [backend]
  (when backend
    (KmsEncryptionConfigBackend. backend)))
