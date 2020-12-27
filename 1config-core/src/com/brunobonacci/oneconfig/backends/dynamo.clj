(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
 com.brunobonacci.oneconfig.backends.dynamo
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory :refer [TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]
            [com.brunobonacci.oneconfig.aws :as aws]
            [clojure.string :as str]))



;;
;;```
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; dynamo-backend
;;  | it stores the entry into DynamoDB table called `1Config` in the given
;;  | region with a conditional insert which it will fail if an entry
;;  | with the same key exists.
;; -----------------------------------------------------------------------------
;;```
;;


(defn default-dynamo-config []
  {:table (config-property
            "1config.dynamo.table"
            "ONECONFIG_DYNAMO_TABLE"
            "1Config")})



(defn- dyn-client
  "returns a cached dynamodb client"
  []
  (aws/make-client (merge (aws/default-cfg) (default-dynamo-config)) :dynamodb))



(defn create-configure-table
  []
  (let [table (:table (default-dynamo-config))
        check (aws/invoke (dyn-client) :DescribeTable {:TableName table})]
    (when (= "com.amazonaws.dynamodb.v20120810#ResourceNotFoundException" (:__type check))
      (aws/invoke! (dyn-client) :CreateTable
        {:TableName table
         :BillingMode "PAY_PER_REQUEST"
         :KeySchema
         [{:AttributeName "__sys_key"  :KeyType "HASH"}
          {:AttributeName "__ver_key"  :KeyType "RANGE"}]
         :AttributeDefinitions
         [{:AttributeName "__sys_key"  :AttributeType "S"}
          {:AttributeName "__ver_key"  :AttributeType "S"}]
         }))
    table))



(defn from-db-rec
  "converts form a dynamo record representation to a clojure map"
  [r]
  (->> r
    (map (fn [[k [& [[t v]]]]] [k (case t :N (Long/parseLong v) v)]))
    (into {})))



(defn to-db-rec
  "converts form a Clojure map to a dynamo record representation"
  [r]
  (->> r
    (map (fn [[k v]] [k {(if (number? v) :N :S) (str v)}]))
    (into {})))



(defn- search-paths
  [version]
  ((juxt identity
     #(subs % 0 6)
     #(subs % 0 3)
     (constantly ""))
   (comparable-version version)))



(defn lazy-paginated-query
  "It creates a generic wrapper for a AWS paginated query"
  [query-fn last-token-name next-token-name result-fn]
  (fn lazy-query
    ([client query]
     (lazy-mapcat result-fn (lazy-query client query nil)))
    ([client query page-token]
     (let [result (query-fn
                    client
                    (cond-> query
                      page-token (assoc next-token-name page-token)))]
       (lazy-seq
         (if-let [next-page (get result last-token-name)]
           (cons result
             (lazy-query client query next-page))
           [result]))))))



(def ^:private lazy-db-scan
  (lazy-paginated-query
    #(aws/invoke! % :Scan %2)
    :LastEvaluatedKey :ExclusiveStartKey :Items))



(deftype DynamoTableConfigBackend [cfg]

  TestStore

  (data [_] nil)

  IConfigClient

  (find [this {:keys [key env version change-num] :as config-entry}]
    (let [zver (comparable-version version)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (or (and change-num (format "%020d" change-num ))
                                   (apply str (repeat 20 "9"))))]
      (some->
        (aws/invoke! (dyn-client) :Query
          {:TableName (:table cfg)
           :Limit 1
           :Select "ALL_ATTRIBUTES"
           :ScanIndexForward false
           :KeyConditions
           {"__sys_key" {:AttributeValueList [{:S sys-key}] :ComparisonOperator "EQ"}
            "__ver_key" {:AttributeValueList [{:S ver-key}] :ComparisonOperator "LE"}}})
        :Items first from-db-rec entry-record)))

  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (let [zver (comparable-version version)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (when change-num (format "%020d" change-num)))]
      (some->
        (aws/invoke! (dyn-client) :Query
          {:TableName (:table cfg)
           :Limit 1
           :Select "ALL_ATTRIBUTES"
           :ScanIndexForward false
           :KeyConditions
           {"__sys_key" {:AttributeValueList [{:S sys-key}] :ComparisonOperator "EQ"}
            "__ver_key" {:AttributeValueList [{:S ver-key}] :ComparisonOperator "BEGINS_WITH"}}})
        :Items first from-db-rec entry-record)))


  (save [this config-entry]
    (let [{:keys [key env version value change-num]
           :as entry} (merge {:content-type "edn"} config-entry)
          zver (comparable-version version)
          change-num (or change-num (System/currentTimeMillis))
          sys-key (str env  "||" key)
          ver-key (str zver "||" (format "%020d" change-num))
          db-entry (assoc entry
                     :__sys_key sys-key
                     :__ver_key ver-key
                     :change-num change-num)]
      (aws/invoke! (dyn-client) :PutItem
        {:TableName (:table cfg)
         :ReturnConsumedCapacity "TOTAL"
         :ReturnItemCollectionMetrics "SIZE"
         :ConditionExpression "attribute_not_exists(#ID)"
         :ExpressionAttributeNames {"#ID" "__sys_key"}
         :Item (to-db-rec db-entry)}))
    this)


  (list [this filters]
    (->> (lazy-db-scan (dyn-client) {:TableName (:table cfg) :Limit 3})
      (map from-db-rec)
      (list-entries filters)
      (map #(assoc % :backend :dynamo)))))



(defn dynamo-config-backend
  [dynamo-config]
  (DynamoTableConfigBackend. dynamo-config))



(comment

  (def d (dynamo-config-backend (default-dynamo-config)))

  (find d {:key "user-service" :env "dev" :version "1.2.9"})

  (list d {})

  (load d {:key "user-service" :env "dev" :version "0.2.0"})



  )
