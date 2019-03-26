(ns com.brunobonacci.oneconfig.cli
  (:refer-clojure :exclude [find load list])
  (:require [cheshire.core :as json]
            [clojure
             [pprint :as pp]
             [string :as str]]
            [clojure.tools.logging :as log]
            [doric.core :as table]
            [safely.core :refer [safely]]
            [com.brunobonacci.oneconfig
             :refer [one-config]]
            [com.brunobonacci.oneconfig
             [backend :refer :all]
             [util :as util]]
            [com.brunobonacci.oneconfig.backends
             [dynamo :as dyn]
             [kms-encryption :as kms]]))



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
     :max-retry 3
     :log-stacktrace false
     :message (str "Creating DynamoDB table:" (:table cfg)))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| S E T |==----                            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti backend (fn [type & opts] type))



(defmethod backend :default
  [type & opts]
  (log/error "Invalid backend type" type)
  (throw (ex-info (str "Invalid backend type " type) {:type type :options opts})))



(defmethod backend :dynamo
  [_ & opts]
  (one-config))



(defn set! [backend config-entry]
  (validate-version! (:version config-entry))
  (safely
   (save backend config-entry)
   :on-error
   :log-stacktrace false
   :message "Saving config entry"))



(defn decode [content-type value]
  (safely
   (util/decode content-type value)
   :on-error
   :log-stacktrace false
   :message (str "parsing value as " content-type)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| G E T |==----                            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- format-value
  [{:keys [content-type value] :as v}]
  (case content-type
    "json"       (json/generate-string value {:pretty true})
    "edn"        (pp/write value :stream nil)
    "txt"        value
    ("properties" "props") (util/properties->str value)))



(defn- format-meta [{:keys [content-type] :as v}]
  (str/join "\n"
            ["-----------------------[META]-----------------------"
             (pp/write (dissoc v :value) :stream nil)
             "----------------------[CONFIG]----------------------"]))



(defn get! [backend config-entry & {:keys [with-meta] :or {with-meta false}}]
  (validate-version! (:version config-entry))
  (safely
   (if-let [result (if (:change-num config-entry)
                     (load backend config-entry)
                     (find backend (dissoc config-entry :change-num)))]
     (do
       (when with-meta
         (println (format-meta result)))
       (println (format-value result)))
     (println "No configuration entry found."))
   :on-error
   :log-stacktrace false
   :message "Retrieving config entry"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| L I S T |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti format-output (fn [{:keys [format backend]} entries] format))



(defmethod format-output :cli
  [{:keys [backend] :as context} entries]
  (->> entries
     (map (fn [{:keys [key env version change-num content-type]}]
            (format "1cfg -b '%s' -k '%s' -e '%s' -v '%s' -t '%s' GET -c '%s' "
                    (name backend) key env version
                    (or content-type "") change-num)))
     (str/join "\n")))



(defmethod format-output :table
  [{:keys [backend] :as context} entries]
  (table/table [{:name :key              :title "Config key"}
                {:name :env              :title "Env"}
                {:name :version          :title "Version"}
                {:name :change-num       :title "Change num"}
                {:name :ts               :title "Timestamp" :format timestamp-format}]
               (->> entries
                  (map (fn [{:keys [change-num] :as m}] (assoc m :ts change-num))))))


(defmethod format-output :tablex
  [{:keys [backend] :as context} entries]
  (table/table [{:name :key              :title "Config key"}
                {:name :env              :title "Env"}
                {:name :version          :title "Version"}
                {:name :change-num       :title "Change num"}
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

  (safely
   (->> (list backend (util/clean-map filters))
      (format-output {:format output-format :backend backend-name})
      println)
   (println "(*) Timestamp is in local time.")
   :on-error
   :log-stacktrace false
   :message "Listing config entry"))


(comment
  (println
   (format-output {:format :cli :backend :dynamo}
                  '({:env "dev", :key "service1", :version "1.0.0", :change-num 123}
                    {:env "dev", :key "service2", :version "1.0.0", :change-num 234234})))

  (list (backend :dynamo) {})
  )




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
