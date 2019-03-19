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
             [backend :refer :all]
             [util :as util]]
            [com.brunobonacci.oneconfig.backends
             [dynamo :as dyn]
             [kms-encryption :as kms]
             [encoding :as coder]]))



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
  (coder/make-encoding-wrapper
   (kms/kms-encryption-backend
    (dyn/dynamo-config-backend (dyn/default-dynamo-config)))))



(defn set! [backend config-entry]
  (validate-version! (:version config-entry))
  (safely
   (save backend config-entry)
   :on-error
   :message "Saving config entry"))



(defn decode [content-type value]
  (safely
   (util/decode content-type value)
   :on-error
   :message (str "parsing value as " content-type)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| G E T |==----                            ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- format-value
  [{:keys [content-type value] :as v}]
  (case content-type
    "application/json" (json/generate-string value {:pretty true})
    "application/edn"  (pp/write value :stream nil)
    "text/plain"       value))



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
   :message "Retrieving config entry"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                            ---==| L I S T |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti format-output (fn [{:keys [format backend]} entries] format))



(def content-map
  {"application/edn"  "edn"
   "application/json" "json"
   "text/plain"       "text"})



(defmethod format-output :cli
  [{:keys [backend] :as context} entries]
  (->> entries
     (map (fn [{:keys [key env version change-num content-type]}]
            (format "1cfg -b '%s' -k '%s' -e '%s' -v '%s' -t '%s' GET -c '%s' "
                    (name backend) key env version
                    (content-map content-type "") change-num)))
     (str/join "\n")))



(defmethod format-output :table
  [{:keys [backend] :as context} entries]
  (table/table [{:name :key              :title "Config key"}
                {:name :env              :title "Env"}
                {:name :version          :title "Version"}
                {:name :change-num       :title "Change num"}
                {:name :master-key-alias :title "Master encryption key"}
                {:name :ts               :title "Timestamp" :format timestamp-format}]
               (map (fn [{:keys [change-num] :as m}] (assoc m :ts change-num)) entries)))



(defn list! [backend filters
             & {:keys [output-format backend-name]
                :or   {output-format :table}}]

  (safely
   (->> (list backend filters)
      (format-output {:format output-format :backend backend-name})
      println)
   :on-error
   :message "Listing config entry"))


(comment
  (println
   (format-output {:format :cli :backend :dynamo}
                  '({:env "dev", :key "service1", :version "1.0.0", :change-num 123}
                    {:env "dev", :key "service2", :version "1.0.0", :change-num 234234})))

  (list (backend :dynamo) {})
  )
