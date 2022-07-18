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

  :javac-options     ["-target" "1.8" "-source" "1.8" ]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60"]

                 ;; avoid conflicts across transitive deps
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.13.3"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.13.3"]
                 [com.fasterxml.jackson.core/jackson-core "2.13.3"]
                 [cheshire "5.11.0"]

                 [com.brunobonacci/oneconfig #=(ver)]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljs-ajax "0.8.4"]
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]

                 [http-kit "2.6.0"]
                 [compojure "1.7.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-defaults "0.3.3"]
                 [ring/ring-json "0.5.1"]
                 [ring-cors "0.1.13"]
                 [enlive "1.1.6"]
                 [reagent "0.10.0"]
                 [re-frame "0.12.0"]

                 [re-frisk "0.5.4.1"]

                 [org.slf4j/slf4j-log4j12 "1.7.36"]
                 [org.apache.logging.log4j/log4j-core "2.18.0"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]]

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
                    :language-in     :ecmascript-next
                    :language-out    :ecmascript-next
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
             :dev     {:dependencies [[figwheel-sidecar "0.5.20"]]
                       :plugins [[lein-figwheel "0.5.19"]
                                 [lein-cljsbuild "1.1.7"]
                                 [lein-binplus "0.6.6"]]}}

  :aliases
  {"start" ["do" "clean," "cljsbuild" "once" "min," "run"]}

  )
