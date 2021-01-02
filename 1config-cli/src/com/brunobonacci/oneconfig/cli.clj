(ns com.brunobonacci.oneconfig.cli
  (:refer-clojure :exclude [find load list])
  (:require [cheshire.core :as json]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.dynamo :as dyn]
            [com.brunobonacci.oneconfig.backends.kms-encryption :as kms]
            [com.brunobonacci.oneconfig.util :as util]
            [com.brunobonacci.oneconfig.profiles :as prof]
            [com.brunobonacci.oneconfig.diff :as diff]
            [doric.core :as table]
            [safely.core :refer [safely]]))



(defn timestamp-format
  [^long ts]
  (format "%1$tF  %1$tT" (java.util.Date. ts)))



(defn- validate-version! [version]
  (when-not (re-find #"^\d+\.\d+\.\d+$" version)
    (throw (ex-info "Version must be in the form: 1.3.23" {}))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                           ---==| I N I T |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti init-backend! (fn [type & opts] type))



(defmethod init-backend! :default
  [type & opts]
  (log/error "Invalid backend type" type)
  (throw (ex-info (str "Invalid backend type " type) {:type type :options opts})))



(defmethod init-backend! :dynamo
  [_ & opts]
  (let [cfg (dyn/default-dynamo-config)]
    (safely
     (dyn/create-configure-table cfg (:table cfg))
     :on-error
     :max-retries 3
     :log-stacktrace false
     :message (str "Creating DynamoDB table:" (:table cfg)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| S E T |==----                            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- validate-backend!
  [backend]
  (when-not backend
    (throw (ex-info "Operation Aborted. Couldn't find a valid backend for this operation."
                    {}))))



(defn- normalize-entry
  [{:keys [content-type] :as config-entry}]
  (as-> config-entry $
    (if (= content-type "props")
      (assoc $ :content-type "properties")
      $)
    (assoc $ :encoded true)))



(defn set! [backend-type backend config-entry]
  (validate-backend! backend)
  (validate-version! (:version config-entry))
  (safely
   (save backend (normalize-entry config-entry))
   :on-error
   :log-stacktrace false
   :message "Saving config entry"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| G E T |==----                            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- format-value
  [{:keys [content-type value encoded-value] :as v} {:keys [pretty-print?]}]
  (if-not pretty-print?
    (or encoded-value value)
    (case content-type
      "json"       (json/generate-string value {:pretty true})
      "yaml"       encoded-value
      "edn"        (pp/write value :stream nil)
      "txt"        value
      ("properties" "props") (util/properties->str value))))



(defn- format-with-meta [{:keys [content-type] :as v} opts]
  (str/join "\n"
            ["-----------------------[META]-----------------------"
             (pp/write (dissoc v :value :encoded-value) :stream nil)
             "----------------------[CONFIG]----------------------"
             (format-value v opts)
             "----------------------------------------------------"]))



(defn- retrieve-entry
  [backend config-entry]
  (let [config-entry (update config-entry :version (fnil identity "99999.99999.99999"))]
    (validate-backend! backend)
    (validate-version! (:version config-entry))
    (safely
     (if (:change-num config-entry)
       (load backend config-entry)
       (find backend (dissoc config-entry :change-num)))
     :on-error
     :log-stacktrace false
     :message "Retrieving config entry")))



(defn get! [backend config-entry & {:keys [with-meta pretty-print?]
                                    :or {with-meta false
                                         pretty-print? false} :as opts}]
  (if-let [result (retrieve-entry backend config-entry)]
    (if with-meta
      (println (format-with-meta result opts))
      (println (format-value result opts)))

    (util/println-err "No configuration entry found.")))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| L I S T |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti format-output (fn [{:keys [format backend]} entries] format))



(defmethod format-output :cli
  [context entries]
  (->> entries
     (map (fn [{:keys [key env version change-num content-type backend] :as e}]
            (format "1cfg -b %s -k %s -e %s -v %s -t %s GET -c %s "
                    (name (or backend (:backend context)))
                    key env version
                    (or content-type "") change-num)))
     (str/join "\n")))



(defmethod format-output :table
  [{:keys [backend] :as context} entries]
  (table/table [{:name :key              :title "Config key"}
                {:name :env              :title "Env"}
                {:name :version          :title "Version"}
                {:name :change-num       :title "Change num"}
                {:name :content-type     :title "Type"}
                {:name :ts               :title "Timestamp" :format timestamp-format}]
               (->> entries
                  (map (fn [{:keys [change-num] :as m}] (assoc m :ts change-num))))))



(defmethod format-output :tablex
  [{:keys [backend] :as context} entries]
  (table/table [{:name :key              :title "Config key"}
                {:name :env              :title "Env"}
                {:name :version          :title "Version"}
                {:name :change-num       :title "Change num"}
                {:name :content-type     :title "Type"}
                {:name :ts               :title "Timestamp" :format timestamp-format}
                {:name :master-key-alias :title "Master encryption key"}
                {:name :user             :title "User"}]
               (->> entries
                  (map (fn [{:keys [change-num] :as m}] (assoc m :ts change-num)))
                  (map (fn [{:keys [master-key-alias master-key] :as m}]
                         (assoc m :master-key-alias (or master-key-alias master-key)))))))



(defn list! [backend filters
             & {:keys [output-format backend-name extended]
                :or   {output-format :table}}]
  (validate-backend! backend)

  (safely
   (->> (list backend (util/clean-map filters))
      (format-output {:format output-format :backend backend-name})
      println)
   (println "(*) Timestamp is in local time.")
   :on-error
   :log-stacktrace false
   :message "Listing config entry"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| L I S T - K E Y S |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn list-keys! []
  (safely
   (println
    (->> (kms/master-keys)
       (map (fn [[k v]] {:alias k :master-key-arn v}))
       (sort-by :alias)
       (table/table [{:name :alias :title "Key alias"}
                     :master-key-arn])))
   :on-error
   :log-stacktrace false
   :message "Listing keys"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                    ----==| C R E A T E - K E Y |==----                     ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn create-key!
  [{:keys [key-name]}]
  (safely
   (let [key-alias (kms/normalize-alias key-name)]
     (->>
      (kms/create-master-key
       key-alias
       (format "1Config managed key for %s configurations" key-name))
      (println "Created key: ")))
   :on-error
   :log-stacktrace false
   :message "Creating key"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                          ----==| D I F F |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn diff! [[backend1 config-entry1] [backend2 config-entry2]
             & {:keys [mode] :or {mode :line}}]
  (let [entry1 (retrieve-entry backend1 config-entry1)
        entry2 (retrieve-entry backend2 config-entry2)]

    (cond
      (not entry1) (util/println-err "Configuration entry not found:" config-entry1)
      (not entry2) (util/println-err "Configuration entry not found:" config-entry2)

      :else
      (let [differ (if (= mode :char) diff/diff-strings diff/diff-lines)
            diffs (differ (:encoded-value entry1) (:encoded-value entry2))]
        (if (> (count diffs) 1)
          (println (diff/colorize-diff diffs))
          (println "No differences found."))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| M I G R A T I O N |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn migrate-entry
  [v1 v2 entry]

  (if (load v2 entry)
    :done
    ;; run the migration
    (try
      (as-> entry $
        (load v1 $)
        (update $ :value (partial kms/decrypt (kms/encryption-context $)))
        (update $ :value (partial kms/encrypt2 (:master-key $) (kms/encryption-context $)))
        (save v2 $)
        ((constantly :done)))
      (catch Exception x
        [:failed (.getMessage ^Exception x)]))))


(defn migrate-table
  [table]
  (let [v1 (dyn/dynamo-config-backend-v1
             (merge (dyn/default-dynamo-config) {:table table}))
        v2 (dyn/dynamo-config-backend-v2
             (merge (dyn/default-dynamo-config) {:table table}))]
    (doseq [{:keys [key version env change-num] :as entry} (list v1 {})]
      (printf " - Migrating %s, %s, %s, %s -> " key version env (str change-num))
      (println (migrate-entry v1 v2 entry)))))


;; (migrate-table "1ConfigMigration")


(defn migrate-database!
  []
  (safely
    (let [table (util/config-property
        "1config.dynamo.table"
        "ONECONFIG_DYNAMO_TABLE"
                  "1Config")]
      (println "Starting migration of:" table)
      (migrate-table table))
    :on-error
    :log-stacktrace false
    :message "Migrating database"))
