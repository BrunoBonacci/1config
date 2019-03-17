(ns com.brunobonacci.oneconfig.backends.kms-encryption
  (:refer-clojure :exclude [find load list])
  (:require [amazonica.aws.kms :as kms]
            [amazonica.core :refer [defcredential] :as aws]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer [lazy-mapcat]]
            [where.core :refer [where]]
            [clojure.core.cache :as cache]
            [clojure.string :as str]
            [amazonica.aws.dynamodbv2 :as dyn])
  (:import [com.amazonaws.encryptionsdk
            AwsCrypto CryptoResult]
           [com.amazonaws.encryptionsdk.kms
            KmsMasterKey KmsMasterKeyProvider]
           [com.amazonaws.regions Regions Region]
           com.amazonaws.PredefinedClientConfigurations
           java.util.Collections))



(def ^:private aws-region
  (delay
   (or
    ;; for dev mode just use `defcredential` macro
    (some-> #'aws/credential deref deref :endpoint Regions/fromName)
    ;; check env
    (some-> (System/getenv "AWS_DEFAULT_REGION") Regions/fromName)
    ;; this call blocks and it is slow on non EC2
    (Regions/getCurrentRegion)
    ;; us-west-2 (??)
    (Regions/DEFAULT_REGION))))



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



(defn- -master-keys
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
   (map (juxt (comp #(subs % 6) :alias-name) :key-arn))
   (into {})))



(def ^:private master-keys-cache
  (atom (cache/ttl-cache-factory {} :ttl 5000)))



(defn master-keys []
  (-> (swap! master-keys-cache cache/through-cache :keys
            (constantly (-master-keys)))
     :keys))



(defn create-master-key
  [key-name description]
  (let [mk (kms/create-key {:description description
                            :key-usage "ENCRYPT_DECRYPT"
                            :origin "AWS_KMS"})]
    (kms/create-alias {:alias-name (format "alias/%s" key-name)
                       :target-key-id (-> mk :key-metadata :key-id)})
    (-> mk :key-metadata :arn)))



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
          ;; reuse amazonica credential variable
          (aws/get-credentials (some-> #'aws/credential deref deref))
          (Region/getRegion @aws-region)
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
          ;; reuse amazonica credential variable
          (aws/get-credentials (some-> #'aws/credential deref deref))
          (Region/getRegion @aws-region)
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



(deftype KmsEncryptionConfigBackend [store]

  IConfigBackend

  (find [this {:keys [key env version] :as config-entry}]
    (when-let [entry (find store config-entry)]
      (let [ctx (select-keys entry [:key :env :version])]
        (update entry :value (comp :result #(decrypt % ctx))))))


  (load [_ {:keys [key env version change-num] :as config-entry}]
    (when-let [entry (load store config-entry)]
      (let [ctx (select-keys entry [:key :env :version])]
        (update entry :value (comp :result #(decrypt % ctx))))))


  (save [_ {:keys [key value] :as config-entry}]
    (let [key-alias  (str "1Config/" key)
          master-key (get (master-keys) key-alias)
          master-key (or master-key
                        (create-master-key
                         key-alias
                         (format "1Config managed key for %s configurations" key)))
          encrypted (:result
                     (encrypt value master-key
                              (select-keys config-entry
                                           [:key :env :version])))]
      (as-> config-entry $
        (assoc $ :value encrypted
               :master-key-alias key-alias
               :master-key master-key)
        (save store $))))

  (list [this filters]
    (list store filters)))



(defn kms-encryption-backend
  [backend]
  (KmsEncryptionConfigBackend. backend))
