(defproject com.brunobonacci/oneconfig (-> "../ver/1config.version" slurp .trim)
  :description "A Clojure library for managing configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :dependencies [[org.clojure/clojure "1.10.2-rc1"]
                 [com.brunobonacci/where "0.5.5"]
                 [org.clojure/tools.logging "1.0.0"]
                 [com.cognitect.aws/api "0.8.484"]
                 [com.cognitect.aws/endpoints "1.1.11.893"]
                 [com.cognitect.aws/sts "809.2.784.0"]
                 [com.cognitect.aws/dynamodb "810.2.801.0"]
                 [com.cognitect.aws/kms "801.2.687.0"]
                 [prismatic/schema "1.1.12"]
                 [cheshire "5.10.0"]
                 [clj-commons/clj-yaml "0.7.0"]]

  :source-paths      ["src" "../1config-shared/src"]
  :java-source-paths ["java/src"]
  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :resource-paths ["resources" "../ver" ]

  :global-vars {*warn-on-reflection* true}

  :profiles {:aot-jar {:aot :all}
             :dev {:dependencies [[midje "1.9.9"]
                                  [org.clojure/test.check "1.0.0"]
                                  [criterium "0.4.5"]
                                  [org.slf4j/slf4j-log4j12 "1.7.30"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.2"]
                                  [lein-shell "0.5.0"]]}}

  ;; generating AOT jar alongside Clojure JAR
  :classifiers {:aot :aot-jar}

  :main com.brunobonacci.main

  :aliases
  {
   "native-config"
   ["shell"
    ;; run the application to infer the build configuration
    "java" "-agentlib:native-image-agent=config-output-dir=./target/config/"
    "-jar" "./target/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "test/service1" "test1" "1.12.0"]

   "native"
   ["shell"
    "native-image" "--report-unsupported-elements-at-runtime" "--no-server" "--no-fallback"
    "-H:+PrintClassInitialization"
    "-H:ConfigurationFileDirectories=./target/config/"
    "--initialize-at-build-time"
    "--allow-incomplete-classpath"
    "--enable-http" "--enable-https" "--enable-all-security-services"
    "-jar" "./target/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:Name=./target/${:name}"]
   }
  )
