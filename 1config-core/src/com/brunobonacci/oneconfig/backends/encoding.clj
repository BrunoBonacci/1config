(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.encoding
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory
             :refer [TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]))

;;
;;```
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; encoding-backend
;;  | it ensures that the `:value` is in the format described by the
;;  | `:content-type`, if-not it encodes it accordingly.
;;  | For example a json value could be passed as a nested map of values
;;  | in such case it will call the appropriate encoder and render a
;;  | the value in the correct format.
;;  | Most of the encoding will return a String representation of the value,
;;  | however the string itself is a value value for edn for example.
;;  | So in order to disambiguate the two cases it looks for an additional
;;  | key callled `:encoded` which can be `true|false` and tells whether
;;  | the given input value is already encoded or not.
;;  | If it is already encoded then the values is decoded to ensure that
;;  | it is valid and avoid malformed input and syntax errors and it is
;;  | finally stored *as it is* which guarantee that comments and formatting
;;  | are preserved.
;;  |
;;  | + {:value (mashall-value value)}
;;  | - {:encoded true|false}
;;```
;;


(deftype EncodingConfigBackend [backend]

  TestStore

  (data [_] (data backend))

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (when-let [{:keys [value] :as entry} (find backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true
                :encoded-value value)))))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (when-let [{:keys [value] :as entry} (load backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true
                :encoded-value value)))))



  (save [this {:keys [encoded] :as config-entry}]
    ;; if an encoded value is provided, verify that is formally correct
    ;; and discard the result. If not correct an exception is raised.
    (when encoded
      (unmarshall-value config-entry))

    ;; Otherwise, if not encoded, encode the value and store it.
    (EncodingConfigBackend.
     (as-> config-entry $
       (dissoc $ :encoded)
       (if-not encoded (marshall-value $) $)
       (save backend $))))

  (list [this filters]
    (list backend filters)))


(defn make-encoding-wrapper [backend]
  (when backend
    (EncodingConfigBackend. backend)))
