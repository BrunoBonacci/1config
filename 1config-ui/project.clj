(defn ver [] (-> "../ver/1config.version" slurp .trim))
(defn java-version
  "It returns the current Java major version as a number"
  []
  (as->  (System/getProperty "java.version") $
    (str/split $ #"\.")
    (if (= "1" (first $)) (second $) (first $))
    (Integer/parseInt $)))

(defproject com.brunobonacci/oneconfig-ui #=(ver)

  :jvm-opts ~(if (>= (java-version) 9)
               ;; Illegal reflective access by com.fasterxml.jackson.databind.util.ClassUtil
               ;; to method java.lang.Throwable.setCause(java.lang.Throwable)
               (vector "--add-opens" "java.base/java.lang=ALL-UNNAMED" "-server")
               (vector "-server"))


  :dependencies [[org.clojure/clojure "1.10.2-rc1"]
                 [org.clojure/clojurescript "1.10.773"]
                 [com.brunobonacci/oneconfig #=(ver)]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljs-ajax "0.8.1"]
                 [cljsjs/react "16.13.0-0"]
                 [cljsjs/react-dom "16.13.0-0"]

                 [http-kit "2.5.0"]
                 [compojure "1.6.2"]
                 [ring "1.8.2"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [ring-cors "0.1.13"]
                 [enlive "1.1.6"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]

                 [re-frisk "0.5.4.1"]

                 [org.slf4j/slf4j-log4j12 "1.7.30"]]

  :min-lein-version "2.7.1"
  :source-paths     ["src" "../1config-shared/src"]
  :main com.brunobonacci.oneconfig.ui.server
  :global-vars {*warn-on-reflection* true}

  :clean-targets ^{:protect false} [:target-path "resources/public/cljs"]
  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src"]
     :figwheel     true
     :compiler     {:main         com.brunobonacci.oneconfig.ui.view
                    :asset-path   "cljs/out"
                    :externs      ["resources/public/js/ace.js"]
                    :foreign-libs [{:file "https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.7/ace.js"
                                    :provides ["ace"]}]
                    :closure-warnings {:externs-validation :off}
                    :output-to  "resources/public/cljs/main.js"
                    :output-dir "resources/public/cljs/out"}}
    {:id           "min"
     :jar          true
     :source-paths ["src"]
     :compiler     {:main          com.brunobonacci.oneconfig.ui.view
                    :output-to     "resources/public/cljs/main.js"
                    :optimizations :advanced
                    :externs       ["resources/public/js/ace.js"]
                    :foreign-libs  [{:file "https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.7/ace.js"
                                     :provides ["ace"]}]
                    :closure-warnings {:externs-validation :off}
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]
   }

  :figwheel {:server-port 5309}


  :bin {:name "1cfg-ui-beta"
        :skip-realign true
        :jvm-opts ["-server" "$JVM_OPTS" "-Dfile.encoding=utf-8"]}


  :profiles {:uberjar {:aot        :all
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]}
             :dev     {:dependencies [[figwheel-sidecar "0.5.19"]]
                       :plugins [[lein-figwheel "0.5.19"]
                                 [lein-cljsbuild "1.1.7"]
                                 [lein-binplus "0.6.6"]]}}

  :aliases
  {"start" ["do" "clean," "cljsbuild" "once" "min," "run"]}

  )
