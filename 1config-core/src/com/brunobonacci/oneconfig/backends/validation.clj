(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.validation
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [schema.core :as s]
            [com.brunobonacci.oneconfig.util :refer [sem-ver]]))

;;
;;```
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; validation-backend
;;  | it ensures that the request (config-entry) has all the necessary
;;  | information and that it is in the expected type/format
;;```
;;


(def ^:private common-name-predicate
  (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" )
          "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))


(def ^:private entry-save-schema
  {:env            common-name-predicate

   :key            common-name-predicate

   :version        (s/pred sem-ver "Version must be of the following form \"1.12.3\"")

   :content-type   (s/enum "txt" "edn" "json" "properties" "props" "yaml")

   :value          s/Str

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

  IConfigClient

  (find [_ config-entry]
    (s/validate entry-request-schema config-entry)
    (find store config-entry))


  IConfigBackend

  (load [_ config-entry]
    (s/validate entry-request-schema config-entry)
    (load store config-entry))


  (save [_ config-entry]
    (s/validate entry-save-schema config-entry)
    (save store config-entry))


  (list [_ filters]
    (s/validate filters-schema filters)
    (list store filters)))



(defn validation-backend
  [store]
  (when store
    (ValidationBackend. store)))
