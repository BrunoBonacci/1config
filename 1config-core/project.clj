(defproject com.brunobonacci/oneconfig (-> "../ver/1config.version" slurp .trim)
  :description "A Clojure library for managing configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.brunobonacci/where "0.5.1"]
                 [com.brunobonacci/safely "0.5.0-alpha6"]
                 [amazonica "0.3.139" :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core     "1.11.513"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.513"]
                 [com.amazonaws/aws-java-sdk-kms      "1.11.513"]
                 [com.amazonaws/aws-java-sdk-iam      "1.11.513"]
                 [com.amazonaws/aws-encryption-sdk-java "1.3.6"]
                 [prismatic/schema "1.1.10"]
                 [cheshire "5.8.1"]]

  :java-source-paths ["java/src"]
  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :resource-paths ["resources" "../ver" ]

  :global-vars {*warn-on-reflection* true}

  :profiles {:dev {:dependencies [[midje "1.9.6"]
                                  [org.clojure/test.check "0.10.0-alpha3"]
                                  [criterium "0.4.4"]
                                  [org.slf4j/slf4j-log4j12 "1.8.0-beta4"]
                                  [lein-binplus "0.6.5"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.1"]]}}
  )
