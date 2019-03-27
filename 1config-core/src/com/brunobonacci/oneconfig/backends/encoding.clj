(ns com.brunobonacci.oneconfig.backends.encoding
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory
             :refer [TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]))



(deftype EncodingConfigBackend [backend]

  TestStore

  (data [_] (data backend))

  IConfigBackend

  (find [this {:keys [key env version] :as config-entry}]
    (when-let [{:keys [value] :as entry} (find backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true
                :encoded-value value)))))



  (load [_ {:keys [key env version change-num] :as config-entry}]
    (when-let [{:keys [value] :as entry} (load backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true
                :encoded-value value)))))



  (save [this {:keys [encoded] :as config-entry}]
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
