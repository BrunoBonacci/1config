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
    (valid-entry-request? config-entry)
    (when-let [entry (find backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true)))))



  (load [_ {:keys [key env version change-num] :as config-entry}]
    (valid-entry-request? config-entry)
    (when-let [entry (load backend config-entry)]
      (if (:decoded entry)
        entry
        (some->
         (unmarshall-value entry)
         (assoc :decoded true)))))



  (save [_ config-entry]
    (valid-entry? config-entry)
    (EncodingConfigBackend.
     (save backend
           (marshall-value config-entry))))

  (list [this filters]
    (list backend filters)))


(defn make-encoding-wrapper [backend]
  (when backend
    (EncodingConfigBackend. backend)))
