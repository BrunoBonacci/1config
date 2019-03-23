(ns com.brunobonacci.oneconfig.backends.hierarchical
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer [list-entries]]))


(deftype HierarchicalBackend [read-stores write-stores]

  IConfigBackend

  (find [_ {:keys [key env version change-num] :as config-entry}]
    (some #(find % config-entry) read-stores))


  (load [_ {:keys [key env version change-num] :as config-entry}]
    (some #(load % config-entry) read-stores))


  (save [_ config-entry]
    (run! #(save % config-entry) write-stores))

  (list [_ filters]
    (->> read-stores
       (mapcat #(list % filters))
       (list-entries filters))))



(defn hierarchical-backend
  [read-stores write-stores]
  (HierarchicalBackend. read-stores write-stores))
