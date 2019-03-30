(ns com.brunobonacci.oneconfig.backends.in-memory
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer :all]))


(defprotocol TestStore

  (data [this]))



(deftype InMemoryConfigBackend [store]

  TestStore

  (data [_] store)

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (let [sem-ver (->> (get-in store [env key])
                        keys
                        (filter #(< (compare % (comparable-version version)) 1))
                        (sort #(compare %2 %1))
                        first)]
      (if-let [changeset (get-in store [env key sem-ver])]
        (get changeset (-> changeset keys ((partial apply max)))))))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (if (nil? change-num)
      (if-let [changeset (get-in store [env key (comparable-version version)])]
        (get changeset (-> changeset keys ((partial apply max)))))
      (get-in store [env key (comparable-version version) change-num])))


  (save [_ config-entry]
    (InMemoryConfigBackend.
     (let [{:keys [key env version value]
            :as entry} (merge {:content-type "edn"} config-entry)
           zver (comparable-version version)]
       (update-in store [env key zver]
                  (fn [ov]
                    (if-not ov
                      {0 (assoc entry :change-num 0 :zver zver)}
                      (let [change-num (-> ov keys ((partial apply max)) inc)]
                        (assoc ov change-num
                               (assoc entry :change-num change-num :zver zver)))))))))
  (list [this filters]
    (->> store
         vals
         (mapcat vals)  ;need to remove the nested structure and just get a seq of enties
         (mapcat vals)
         (mapcat vals)
         (list-entries filters))))



(defn in-memory-config-backend
  []
  (InMemoryConfigBackend. {}))
