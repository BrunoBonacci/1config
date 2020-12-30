(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.kms-encryption
  (:refer-clojure :exclude [find load list])
  (:require [amazonica.aws.kms :as kms]
            [amazonica.core :as aws]
            [clojure.string :as str]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer [clean-map lazy-mapcat env]]
            [where.core :refer [where]]
            [com.brunobonacci.oneconfig.util :refer [safely]]
            [clojure.java.io :as io])
  (:import com.amazonaws.encryptionsdk.AwsCrypto
           com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider
           com.amazonaws.PredefinedClientConfigurations
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
    ;; for dev mode just use `defcredential` macro
    (some-> #'aws/credential deref deref :endpoint Regions/fromName Region/getRegion)
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



(defn- list-aliases
  [start-from]
  (kms/list-aliases (if start-from {:marker start-from} {})))


;; lazy wrapper for query
(defn lazy-list-with-marker
  "Takes a query as a lambda function and returns
   a lazy pagination over the items"
  ([top-key q]
   ;; mapcat is not lazy so defining one
   (lazy-mapcat top-key (lazy-list-with-marker :first-page q nil)))
  ;; paginate lazily the query
  ([_ q start-from]
   (let [result (q start-from)]
     (lazy-seq
      (if-let [next-page (:next-marker result)]
        (cons result
              (lazy-list-with-marker :next-page q next-page))
        [result])))))



(defn master-keys
  []
  (->>
   (lazy-list-with-marker :aliases list-aliases)
   (filter (where :alias-name :starts-with? "alias/1Config/"))
   ;; build key ARN out of key alias.
   (map (fn [{:keys [target-key-id alias-arn alias-name] :as m}]
          (let [arn
                (->
                 (re-find (re-pattern (str "(.*):\\Q" alias-name "\\E")) alias-arn)
                 second
                 (str ":key/" target-key-id))]
            (assoc m :key-arn arn))))
   (map (juxt :alias-name :key-arn))
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
  (let [mk (kms/create-key {:description description
                            :key-usage "ENCRYPT_DECRYPT"
                            :origin "AWS_KMS"})]
    (kms/create-alias {:alias-name (normalize-alias key-name)
                       :target-key-id (-> mk :key-metadata :key-id)})
    (-> mk :key-metadata :arn)))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;       ----==| N E W   V I A   K M S   D I R E C T   C A L L |==----        ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- base64-decode
  [stream]
  (let [baos (java.io.ByteArrayOutputStream.)]
    (io/copy stream baos)
    (java.nio.ByteBuffer/wrap (.decode (java.util.Base64/getDecoder) ^bytes (.toByteArray baos)))))


(defn decrypt2
  [context text]
  (->>
    (kms/decrypt
      :encryption-context (into {} (map (fn [[k v]] [(str k) (str v)]) context))
      :ciphertext-blob (base64-decode text))
    :plaintext
    ((memfn ^java.nio.ByteBuffer array))
    (#(String. ^bytes % "utf-8"))))



(defn- base64-encode
  [^java.nio.ByteBuffer bytes]
  (let [blob (.array bytes)]
    (String. (.encode (java.util.Base64/getEncoder) blob) "utf-8")))

(defn encrypt2
  [master-key-id context text]
  (->
    (kms/encrypt
      :key-id master-key-id
      :encryption-context (into {} (map (fn [[k v]] [(str k) (str v)]) context))
      :encryption-algorithm "SYMMETRIC_DEFAULT"
      :plaintext text)
    :ciphertext-blob
    base64-encode))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;    ----==| O L D   V I A   A W S - E N C R Y P T I O N - S D K |==----     ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; AWS Cryto utility
(defonce ^:private ^AwsCrypto crypto
  (AwsCrypto.))



(defn encrypt
  [master-key-id context ^String payload]
  (let [ ;; retrieving the master key to generate the data key
        ^KmsMasterKeyProvider master-key
        (KmsMasterKeyProvider.
          ;; reuse amazonica credential variable
          (aws/get-credentials (some-> #'aws/credential deref deref))
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
    (.getResult out)))



(defn decrypt
  [context ^String payload]
  (let [ ;; retrieving the master key to generate the data key
        ^KmsMasterKeyProvider master-key
        (KmsMasterKeyProvider.
          ;; reuse amazonica credential variable
          (aws/get-credentials (some-> #'aws/credential deref deref))
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
    (.getResult out)))



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
          master-key-arn (-> (kms/describe-key {:key-id master-key}) :key-metadata :arn)
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



(defn encryption-context
  [config-entry]
  (select-keys config-entry [:key :env :version :change-num :content-type :user]))



(deftype KmsEncryptionConfigBackend [store]

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (when-let [entry (find store config-entry)]
      (let [ctx (encryption-context entry)]
        (update entry :value #(decrypt ctx %)))))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (when-let [entry (load store config-entry)]
      (let [ctx (encryption-context entry)]
        (update entry :value #(decrypt ctx %)))))


  (save [_ {:keys [key value master-key] :as config-entry}]
    (let [{:keys [master-key-alias master-key]
           } (resolve-master-key (or master-key key))
          context (encryption-context config-entry)
          encrypted (encrypt master-key context value)]
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
