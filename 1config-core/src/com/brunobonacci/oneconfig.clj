(ns ^{:author "Bruno Bonacci (@BrunoBonacci)"
      :doc "A library to manage application secrets and configuration safely and effectively."}
    com.brunobonacci.oneconfig
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends :refer [backend-factory]]
            [com.brunobonacci.oneconfig.util
             :refer [sem-ver log-configure-request]]))


(defonce ^:private ^com.brunobonacci.oneconfig.backend.IConfigClient one-config-client
  (backend-factory {:type :default}))


(defn configure
  "Returns a configuration entry if found in any of the available
  configuration backends or `nil` if not found.

  example:
  ``` clojure
  (configure {:key \"service1\" :env \"dev\" :version \"6.2.4\"})

  ;; => {:key \"service1\",
  ;;      :env \"dev\",
  ;;      :version \"1.0.0\",
  ;;      :change-num 1577379732258,
  ;;      :content-type \"edn\",
  ;;      :value {:config \"the actual config\" :password \"S3cret\"},
  ;;      :master-key-alias \"alias/1Config/service1\",
  ;;      :master-key \"arn:aws:kms:eu-west-1:1234567890:key/09f50161-01e7-44f1-9b39-fac39c7267eb\",
  ;;      :user \"arn:aws:iam::1234567890:user/john.doe\"}
  ```

  When config is not found:
  ``` clojure
  (configure {:key \"system-xyz\" :env \"dev\" :version \"6.2.4\"})
  ;; => nil
  ```

  Typical usage:

  ``` clojure
  ;; read the config and merge with defaults
  (->> (configure
        {:key \"user-service\"
         :env (or (System/getenv \"ENV\") \"local\")
         :version \"6.2.4\"})
       :value
       (deep-merge DEFAULT-CONFIG))
  ```

  for more information check the best practices:
  https://github.com/BrunoBonacci/1config/doc/best-practices.md
  and the general documentation:
  https://github.com/BrunoBonacci/1config
  "
  ([{:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (configure one-config-client config-entry))
  ([^com.brunobonacci.oneconfig.backend.IConfigClient config-client
    {:keys [env key version] :as config-entry}]
   {:pre [env key (sem-ver version)]}
   (log-configure-request config-entry
     (when config-client
       (find config-client config-entry)))))


;; (configure {:key "service1" :env "dev" :version "6.2.4"})


(defn deep-merge
  "Like merge, but merges maps recursively. It merges the maps from left
  to right and the right-most value wins. It is useful to merge the
  user defined configuration on top of the default configuration."
  [& maps]
  (let [maps (filter (comp not nil?) maps)]
    (if (every? map? maps)
      (apply merge-with deep-merge maps)
      (last maps))))
