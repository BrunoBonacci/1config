(defproject com.brunobonacci/oneconfig (-> "../ver/1config.version" slurp .trim)
  :description "A Clojure library for managing configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.brunobonacci/where "0.5.6"]
                 [org.clojure/tools.logging "1.2.4"]
                 [com.cognitect.aws/api "0.8.568"]
                 [com.cognitect.aws/endpoints "1.1.12.257"]
                 [com.cognitect.aws/sts "822.2.1145.0"]
                 [com.cognitect.aws/dynamodb "822.2.1171.0"]
                 [com.cognitect.aws/kms "822.2.1145.0"]
                 [prismatic/schema "1.3.0"]
                 [com.cnuernber/charred "1.011"]
                 [clj-commons/clj-yaml "0.7.108"]]

  :source-paths      ["src" "../1config-shared/src"]
  :java-source-paths ["java/src"]
  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :resource-paths ["resources" "../ver" ]

  :global-vars {*warn-on-reflection* true}

  :profiles {:aot-jar {:aot :all}
             :dev {:dependencies [[midje "1.10.5"]
                                  [org.clojure/test.check "1.1.1"]
                                  [criterium "0.4.6"]
                                  [org.slf4j/slf4j-log4j12 "1.7.36"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.2"]
                                  [lein-shell "0.5.0"]]}}

  ;; generating AOT jar alongside Clojure JAR
  :classifiers {:aot :aot-jar}

  )
