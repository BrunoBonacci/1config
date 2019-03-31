(ns com.brunobonacci.oneconfig
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends :refer [backend-factory]]
            [com.brunobonacci.oneconfig.util :refer :all]))


(defonce ^:private ^com.brunobonacci.oneconfig.backend.IConfigClient one-config-client
  (backend-factory {:type :default}))


(defn configure
  "Returns a configuration entry if found in any
   of the available configuration backends or nil
   if not found"
  ([{:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (configure one-config-client config-entry))
  ([^com.brunobonacci.oneconfig.backend.IConfigClient config-client
    {:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (log-configure-request config-entry
     (when config-client
       (find config-client config-entry)))))


;; (configure {:key "system1" :env "dev" :version "6.2.4"})


(defn deep-merge
  "Like merge, but merges maps recursively. It merges the maps from left
  to right and the right-most value wins. It is useful to merge the
  user defined configuration on top of the default configuration."
  [& maps]
  (let [maps (filter (comp not nil?) maps)]
    (if (every? map? maps)
      (apply merge-with deep-merge maps)
      (last maps))))
