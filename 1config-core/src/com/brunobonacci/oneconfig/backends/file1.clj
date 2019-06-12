(ns com.brunobonacci.oneconfig.backends.file1
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends.in-memory
             :refer [in-memory-config-backend TestStore data]]
            [com.brunobonacci.oneconfig.util :refer :all]))


(deftype ReadOnlySingleFileConfigBackend [file type content]

  TestStore

  (data [_] {:file file :type type :content content})

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (assoc config-entry
           :value content
           :content-type type
           :change-num (System/currentTimeMillis)))

  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (assoc config-entry
           :value content
           :content-type type
           :change-num (System/currentTimeMillis)))


  (save [_ config-entry]
    (throw (ex-info "Operation not permitted on this type of backend."
                    {:type "ReadOnlySingleFileConfigBackend" :file file})))

  (list [this filters]
    [{:env "*"
      :key "*"
      :version "*"
      :content-type type
      :change-num (System/currentTimeMillis)
      :backend :fix}]))



(defn file1-config-backend
  [file]
  (let [type    (or (filename->content-type (str file))
                   (throw (ex-info "Unrecognised configuration file format."
                                   {:type "ReadOnlySingleFileConfigBackend" :file file})))

        content (or (read-config-file file)
                   (throw (ex-info "Unable to load configuration file."
                                   {:type "ReadOnlySingleFileConfigBackend" :file file})))]
    (ReadOnlySingleFileConfigBackend. file type content)))


(comment

  (def c (file1-config-backend "/tmp/test.txt"))

  (data c)

  (list c {:env "dev"})

  (find c {:key "amazon_password" :version "2.2.2" :env "dev"})
  )
