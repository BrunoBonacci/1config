(ns com.brunobonacci.oneconfig.backends.immutable
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]))


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
  (ImmutableBackend. store))
