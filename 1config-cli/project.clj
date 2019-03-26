(defn ver [] (-> "../ver/1config.version" slurp .trim))
(defproject com.brunobonacci/oneconfig-cli #=(ver)
  :description "A command line utility for managing 1config configurations"

  :url "https://github.com/BrunoBonacci/1config"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/1config.git"}

  :main com.brunobonacci.oneconfig.main

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.brunobonacci/oneconfig #=(ver)]
                 [org.clojure/tools.cli "0.4.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.26"]
                 [doric "0.9.0"]
                 [com.brunobonacci/safely "0.5.0-alpha6"]]

  :global-vars {*warn-on-reflection* true}

  :jvm-opts ["-server"]

  :resource-paths ["resources" "../ver" ]

  :bin {:name "1cfg"
        :bin-path "~/bin"
        :jvm-opts ["-server" "$JVM_OPTS" "-Dfile.encoding=utf-8"]}

  :profiles {:dev {:dependencies [[midje "1.9.6"]
                                  [org.clojure/test.check "0.10.0-alpha3"]
                                  [criterium "0.4.4"]
                                  [org.slf4j/slf4j-log4j12 "1.8.0-beta4"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.1"]
                                  [lein-binplus "0.6.5"]]}}
  )
