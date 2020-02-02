(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.backends.immutable
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]))

;;
;;```
;;  |
;;  V
;; -|---------------------------------------------------------------------------
;; immutable-backend
;;  | it ensures that a new `change-num` is set so that if it is present in
;;  | the user request it will be overwritten with a value given by the system.
;;  | This ensures that if a config entry if returned from a GET operation,
;;  | updated and the SET (stored) a new config-entry will be created.
;;  |
;;  | + {:change-num (new-num)}
;;```
;;

(deftype ImmutableBackend [store]

  IConfigClient

  (find [_ {:keys [key env version change-num] :as config-entry}]
    (find store config-entry))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (load store config-entry))


  (save [_ config-entry]
    ;; overrides any previously present :change-num
    (->> (assoc config-entry :change-num (System/currentTimeMillis))
         (save store)))

  (list [_ filters]
    (list store filters)))



(defn immutable-backend
  [store]
  (when store
    (ImmutableBackend. store)))
