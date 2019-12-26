(ns com.brunobonacci.oneconfig.ui.utils
  (:require [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [clojure.string :as string]
            [ajax.core :refer [GET POST]]
            [where.core :refer [where]]))



(def one-config-formatter (tf/formatter "yyyy-MM-dd HH:mm:ss"))


(defn parse-date [timestamp]
  (->> (tc/from-long timestamp)
       (t/to-default-time-zone)
       (tf/unparse one-config-formatter)))



;; TODO: add ability to customise color coding via user-profiles
(defn colourize-label
  "find a color for a label by environment"
  [env]
  (cond
    (= env "local")   "white"
    (= env "dev")     "green"
    (= env "test")    "blue"
    (= env "uat")     "yellow"
    (= env "staging") "yellow"
    (= env "prod")    "red"
    (= env "prd")     "red"
    :else             "grey"))



(defn as-label
  ([value]            [:div.ui.label {:class ""} value])
  ([css-class value]  [:div.ui.label {:class css-class} value]))



(defn as-code [value]
  [:pre [:code value]])



(defn get-last-of-splitted [line re]
  (last (string/split line re)))



(defn get-extension [file-name]
  (get-last-of-splitted file-name #"\."))



(defn get-aws-username [assume-role-user-line]
  (get-last-of-splitted assume-role-user-line #"/"))



(defn get-kms-uuid [kms-line]
  (get-last-of-splitted kms-line #"/"))



(defn filter-entries [{:keys [key env version]} entries]
  (->> entries
       (filter
        (where [:and
                [:env     :CONTAINS? (or env "")]
                [:key     :CONTAINS? (or key "")]
                [:version :starts-with? (or version "")]]))))
