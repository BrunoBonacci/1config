(defn ver [] (-> "../ver/1config.version" slurp .trim))
(defn java-version
  "It returns the current Java major version as a number"
  []
  (as->  (System/getProperty "java.version") $
    (str/split $ #"\.")
    (if (= "1" (first $)) (second $) (first $))
    (Integer/parseInt $)))
(defproject com.brunobonacci/oneconfig-cli #=(ver)
  :description "A command line utility for managing 1config configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :main com.brunobonacci.oneconfig.main

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.brunobonacci/oneconfig #=(ver)]
                 [org.clojure/tools.cli "1.0.194"]
                 [org.slf4j/slf4j-log4j12 "1.7.30"]
                 [doric "0.9.0"]
                 [com.brunobonacci/safely "0.5.0"]]

  :global-vars {*warn-on-reflection* true}

  :jvm-opts ~(if (>= (java-version) 9)
               ;; Illegal reflective access by com.fasterxml.jackson.databind.util.ClassUtil
               ;; to method java.lang.Throwable.setCause(java.lang.Throwable)
               (vector "--add-opens" "java.base/java.lang=ALL-UNNAMED" "-server")
               (vector "-server"))

  :java-source-paths ["java/src"]
  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :resource-paths ["resources" "../ver" ]

  :bin {:name "1cfg"
        :jvm-opts ["-server" "$JVM_OPTS" "-Dfile.encoding=utf-8"]}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.9.9"]
                                  [org.clojure/test.check "1.0.0"]
                                  [criterium "0.4.5"]
                                  [org.slf4j/slf4j-log4j12 "1.7.30"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.2"]
                                  [lein-shell "0.5.0"]
                                  [lein-binplus "0.6.6"]]}}

  :aliases
  {"package"
   ["do" "shell" "./bin/package.sh"]}
  )
