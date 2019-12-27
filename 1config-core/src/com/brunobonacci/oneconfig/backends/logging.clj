(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.logging
  (:refer-clojure :exclude [find load list])
  (:require [clojure.tools.logging :as log]
            [com.brunobonacci.oneconfig.backend :refer :all]))

(deftype LoggingBackend [store]

  IConfigClient

  (find [_ {:keys [key env version change-num] :as config-entry}]
    (let [result (find store config-entry)]
      (log/info "FIND:" (pr-str config-entry) "->" (pr-str result))
      result))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (let [result (load store config-entry)]
      (log/info "LOAD:" (pr-str config-entry) "->" (pr-str result))
      result))


  (save [_ config-entry]
    (log/info "SAVE:" (pr-str config-entry))
    (save store config-entry))

  (list [_ filters]
    (log/info "LIST:" (pr-str filters))
    (list store filters)))



(defn logging-backend
  [store]
  (when store
    (LoggingBackend. store)))
