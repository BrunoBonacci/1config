(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.dynamo
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory :refer [TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]
            [amazonica.aws.dynamodbv2 :as dyn]
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
  {:endpoint  nil ;; this is required to by amazonica
                  ;; to consider first argument a config
   :table     (config-property "1config.dynamo.table"
                               "ONECONFIG_DYNAMO_TABLE"
                               "1Config")})


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

  IConfigClient

  (find [this {:keys [key env version change-num] :as config-entry}]
    (let [zver (comparable-version version)
          sys-key (str env  "||" key)
          ver-key (str zver "||" (or (and change-num (format "%020d" change-num ))
                                    (apply str (repeat 20 "9"))))]
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

  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
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
      (dyn/put-item cfg
                    :table-name (:table cfg)
                    :return-consumed-capacity "TOTAL"
                    :return-item-collection-metrics "SIZE"
                    :condition-expression "attribute_not_exists(#ID)"
                    :expression-attribute-names {"#ID" "__sys_key"}
                    :item db-entry))
    this)


  (list [this filters]
    (let [q (fn [start-from]
              (dyn/scan cfg
                        (if start-from
                          {:table-name (:table cfg) :exclusive-start-key start-from}
                          {:table-name (:table cfg)})))]
      (->> (lazy-query q)
         (list-entries filters)
         (distinct) ;; remove duplicates caused by migration
         (map #(assoc % :backend :dynamo))))))


(deftype DynamoTableConfigBackendV2 [cfg]

  TestStore

  (data [_] nil)

  IConfigClient

  (find [this {:keys [key env version change-num] :as config-entry}]
    (let [zver (comparable-version version)
          sys-key (str "!2||" env  "||" key)
          ver-key (str zver "||" (or (and change-num (format "%020d" change-num ))
                                    (apply str (repeat 20 "9"))))]
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

  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (let [zver (comparable-version version)
          sys-key (str "!2||" env  "||" key)
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
    (let [{:keys [key env version value change-num]
           :as entry} (merge {:content-type "edn"} config-entry)
          zver (comparable-version version)
          change-num (or change-num (System/currentTimeMillis))
          sys-key (str "!2||" env  "||" key)
          ver-key (str zver "||" (format "%020d" change-num))
          db-entry (assoc entry
                          :__sys_key sys-key
                          :__ver_key ver-key
                          :change-num change-num)]
      (dyn/put-item cfg
                    :table-name (:table cfg)
                    :return-consumed-capacity "TOTAL"
                    :return-item-collection-metrics "SIZE"
                    :condition-expression "attribute_not_exists(#ID)"
                    :expression-attribute-names {"#ID" "__sys_key"}
                    :item db-entry))
    this)


  (list [this filters]
    (let [q (fn [start-from]
              (dyn/scan cfg
                        (if start-from
                          {:table-name (:table cfg) :exclusive-start-key start-from}
                          {:table-name (:table cfg)})))]
      (->> (lazy-query q)
         (list-entries filters)
         (distinct) ;; remove duplicates caused by migration
         (map #(assoc % :backend :dynamo))))))



(defn dynamo-config-backend
  [dynamo-config]
  (DynamoTableConfigBackend. dynamo-config))

(defn dynamo-config-backend-v2
  [dynamo-config]
  (DynamoTableConfigBackendV2. dynamo-config))
