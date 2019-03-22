(ns com.brunobonacci.oneconfig.backends.logging
  (:refer-clojure :exclude [find load list])
  (:require [clojure.tools.logging :as log]
            [com.brunobonacci.oneconfig.backend :refer :all]))

(deftype LoggingBackend [store]

  IConfigBackend

  (find [_ {:keys [key env version change-num] :as config-entry}]
    (log/info "FIND:" (pr-str config-entry))
    (find store config-entry))


  (load [_ {:keys [key env version change-num] :as config-entry}]
    (log/info "LOAD:" (pr-str config-entry))
    (load store config-entry))


  (save [_ config-entry]
    (log/info "SAVE:" (pr-str config-entry))
    (save store config-entry))

  (list [_ filters]
    (log/info "LIST:" (pr-str filters))
    (list store filters)))



(defn logging-backend
  [store]
  (LoggingBackend. store))
