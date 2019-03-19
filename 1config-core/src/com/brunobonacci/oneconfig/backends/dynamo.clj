(ns com.brunobonacci.oneconfig.backends.dynamo
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory :refer [TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]
            [amazonica.aws.dynamodbv2 :as dyn]
            [clojure.string :as str]))


(defn default-dynamo-config []
  ;; TODO: REGION????
  {:endpoint  nil ;;(or (System/getenv "AWS_DEFAULT_REGION") "eu-west-1")
   :table     (or (System/getenv "ONECONFIG_DYNAMO_TABLE") "1Config")})


(defn create-configure-table
  [aws-config tablename]
  (dyn/create-table
   (or aws-config (default-dynamo-config))
   :table-name (or tablename (:table (default-dynamo-config)))
   :key-schema
   [{:attribute-name "__sys_key"  :key-type "HASH"}
    {:attribute-name "__ver_key"  :key-type "RANGE"}]
   :attribute-definitions
   [{:attribute-name "__sys_key"  :attribute-type "S"}
    {:attribute-name "__ver_key"  :attribute-type "S"}]
   :provisioned-throughput
   {:read-capacity-units 10
    :write-capacity-units 10}))


(defn- search-paths
  [version]
  ((juxt identity
         #(subs % 0 6)
         #(subs % 0 3)
         (constantly ""))
   (comparable-version version)))


(defn lazy-query
  "Takes a query as a lambda function and retunrs
   a lazy pagination over the items"
  ([q]
   ;; mapcat is not lazy so defining one
   (lazy-mapcat :items (lazy-query q nil)))
  ;; paginate lazily the query
  ([q start-from]
   (let [result (q start-from)]
     (lazy-seq
      (if-let [next-page (:last-evaluated-key result)]
        (cons result
              (lazy-query q next-page))
        [result])))))


(deftype DynamoTableConfigBackend [cfg]

  TestStore

  (data [_] nil)

  IConfigBackend

  (find [this {:keys [key env version change-num] :as config-entry}]
    (valid-entry-request? config-entry)
    (let [zver (comparable-version version)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (format "%020d" (or change-num (System/currentTimeMillis))))]
      (some->
       (dyn/query cfg
                  :table-name (:table cfg)
                  :limit 1
                  :select "ALL_ATTRIBUTES"
                  :scan-index-forward false
                  :key-conditions
                  {:__sys_key {:attribute-value-list [sys-key] :comparison-operator "EQ"}
                   :__ver_key {:attribute-value-list [ver-key] :comparison-operator "LE"}})
       :items first entry-record)))


  (load [_ {:keys [key env version change-num] :as config-entry}]
    (valid-entry-request? config-entry)
    (let [zver (comparable-version version)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (when change-num (format "%020d" change-num)))]
      (some->
       (dyn/query cfg
                  :table-name (:table cfg)
                  :limit 1
                  :select "ALL_ATTRIBUTES"
                  :scan-index-forward false
                  :key-conditions
                  {:__sys_key {:attribute-value-list [sys-key] :comparison-operator "EQ"}
                   :__ver_key {:attribute-value-list [ver-key] :comparison-operator "BEGINS_WITH"}})
       :items first entry-record)))


  (save [this config-entry]
    (valid-entry? config-entry)
    (let [{:keys [key env version value]
           :as entry} (merge {:content-type "edn"} config-entry)
          zver (comparable-version version)
          change-num (System/currentTimeMillis)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (format "%020d" change-num))
          db-entry (assoc entry
                          :__sys_key sys-key
                          :__ver_key ver-key
                          :change-num change-num)]
      (dyn/put-item cfg
                    :table-name (:table cfg)
                    :return-consumed-capacity "TOTAL"
                    :return-item-collection-metrics "SIZE"
                    :item db-entry))
    this)

  (list [this filters]
    (let [q (fn [start-from]
              (dyn/scan cfg
                        (if start-from
                          {:table-name (:table cfg) :exclusive-start-key start-from}
                          {:table-name (:table cfg)})))]
      (->> (lazy-query q)
           (list-entries filters)))))



(defn dynamo-config-backend
  [dynamo-config]
  (DynamoTableConfigBackend. dynamo-config))
