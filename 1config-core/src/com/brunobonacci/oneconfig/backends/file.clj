(ns com.brunobonacci.oneconfig.backends.file
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory
             :refer [in-memory-config-backend TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]))


(deftype ReadOnlyFileConfigBackend [file content store]

  TestStore

  (data [_] {:file file :content content :store (data store)})

  IConfigBackend

  (find [this {:keys [key env version] :as config-entry}]
    (find store config-entry))


  (load [_ {:keys [key env version change-num] :as config-entry}]
    (load store config-entry))


  (save [_ config-entry]
    (throw (ex-info "Operation not permitted on this type of backend."
                    {:type "ReadOnlyFileBased" :file file})))

  (list [this filters]
    (list store filters)))



(defn readonly-file-config-backend
  [file]
  (let [content (or (read-edn-file file)
                   (throw (ex-info "Unable to configuration file."
                                   {:type "ReadOnlyFileBased" :file file})))
        store (in-memory-config-backend)]
    (ReadOnlyFileConfigBackend. file content
                                (->> content
                                     (map (partial merge {:content-type "edn"}))
                                     (map marshall-value)
                                     (reduce save store)))))


(comment

  (def c (readonly-file-config-backend (str (homedir) "/.1config/1config.edn")))

  (data c)

  (list c {:env "dev"})

  (find c {:key "amazon_password" :version "2.2.2" :env "dev"})
  )
