(defproject com.brunobonacci/oneconfig (-> "../ver/1config.version" slurp .trim)
  :description "A Clojure library for managing configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :dependencies [[org.clojure/clojure "1.10.2-rc1"]
                 [com.brunobonacci/where "0.5.5"]
                 [org.clojure/tools.logging "1.1.0"]
                 [com.cognitect.aws/api "0.8.484"]
                 [com.cognitect.aws/endpoints "1.1.11.926"]
                 [com.cognitect.aws/sts "809.2.784.0"]
                 [com.cognitect.aws/dynamodb "810.2.801.0"]
                 [com.cognitect.aws/kms "810.2.817.0"]
                 [prismatic/schema "1.1.12"]
                 [metosin/jsonista "0.3.0"]
                 [clj-commons/clj-yaml "0.7.2"]]

  :source-paths      ["src" "../1config-shared/src"]
  :java-source-paths ["java/src"]
  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :resource-paths ["resources" "../ver" ]

  :global-vars {*warn-on-reflection* true}

  :profiles {:aot-jar {:aot :all}
             :dev {:dependencies [[midje "1.9.9"]
                                  [org.clojure/test.check "1.1.0"]
                                  [criterium "0.4.6"]
                                  [org.slf4j/slf4j-log4j12 "1.7.30"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.2"]
                                  [lein-shell "0.5.0"]]}}

  ;; generating AOT jar alongside Clojure JAR
  :classifiers {:aot :aot-jar}

  )
