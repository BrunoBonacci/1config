(ns com.brunobonacci.oneconfig.ui.utils
  (:require
    [cljs-time.core :as t]
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


;; TODO need a configuration point which could maintain correspondence
(defn colourize-label
  "find a color for a label by environment"
  [env]
  (cond
    (= env "test") "yellow horizontal"
    (= env "prod") "red horizontal"
    (= env "staging") "green horizontal"
    (= env "beta") "blue horizontal"
    :else "grey horizontal"))

(defn as-label
  ([value]            [:div.ui.label {:class ""} value])
  ([css-class value]  [:div.ui.label {:class css-class} value]))

(defn as-code [value]
  [:pre
   [:code value]])


(defn get-last-of-splitted [line re]
  (last (string/split line re))
  )

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
                 [:env     :starts-with? (or env "")]
                 [:key     :starts-with? (or key "")]
                 [:version :starts-with? (or version "")]]))))