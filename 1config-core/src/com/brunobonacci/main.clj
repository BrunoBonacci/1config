(ns com.brunobonacci.main
  (:refer-clojure :exclude [find load list])
  (:require [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backends :as b]
            [cognitect.aws.protocols.json]
            [cognitect.aws.protocols.common]
            [clojure.core.specs.alpha])
  (:gen-class))



(defn -main
  [k e v]
  (println "loading....")
  (prn (find (b/backend-factory {:type :hierarchical :force true}) {:key k :env e :version v})))
