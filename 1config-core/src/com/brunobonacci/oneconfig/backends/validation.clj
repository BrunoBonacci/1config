(ns com.brunobonacci.oneconfig.backends.validation
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [schema.core :as s]
            [com.brunobonacci.oneconfig.util :refer [sem-ver]]))

(def ^:private common-name-predicate
  (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" )
          "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))


(def ^:private entry-save-schema
  {:env            common-name-predicate

   :key            common-name-predicate

   :version        (s/pred sem-ver "Version must be of the following form \"1.12.3\"")

   :content-type   (s/enum "txt" "edn" "json" "properties" "props")

   :value          s/Any

   (s/optional-key :master-key) s/Str})



(def ^:private entry-request-schema
  {:env common-name-predicate

   :key common-name-predicate

   :version (s/pred sem-ver "Version must be of the following form \"1.12.3\"")

   (s/optional-key :change-num) s/Int})



(def ^:private filters-schema
  {(s/optional-key :env) (s/maybe s/Str)

   (s/optional-key :key) (s/maybe s/Str)

   (s/optional-key :version) (s/maybe s/Str)

   (s/optional-key :order-by) (s/maybe [(s/enum :key :env :version :change-num)])})



(deftype ValidationBackend [store]

  IConfigBackend

  (find [_ config-entry]
    (s/validate entry-request-schema config-entry)
    (find store config-entry))


  (load [_ config-entry]
    (s/validate entry-request-schema config-entry)
    (load store config-entry))


  (save [_ config-entry]
    (s/validate entry-save-schema config-entry)
    ;; overrides any previously present :change-num
    (->> (assoc config-entry :change-num (System/currentTimeMillis))
       (save store)))


  (list [_ filters]
    (s/validate filters-schema filters)
    (list store filters)))



(defn validation-backend
  [store]
  (ValidationBackend. store))
