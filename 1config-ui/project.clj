(defproject oneconfig-ui "0.1.0-SNAPSHOT"

  :jvm-opts ~(let [version (System/getProperty "java.version")
                   [major _ _] (clojure.string/split version #"\.")]
               (if (>= (java.lang.Integer/parseInt major) 9)
                 ["--add-modules" "java.xml.bind"]
                 []))

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.4.474"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/react "15.0.0-0"]
                 [cljsjs/react-dom "15.0.0-0"]

                 [cheshire "5.8.1"]
                 [com.brunobonacci/oneconfig "0.10.2"]

                 [http-kit "2.2.0-alpha1"]
                 [compojure "1.6.0"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.2.1"]
                 [ring/ring-json "0.3.1"]
                 [ring-cors "0.1.13"]
                 [enlive "1.1.5"]
                 [reagent "0.7.0"]
                 [re-frame "0.10.6"]

                 [re-frisk "0.3.1"]
                 [garden "1.3.2"]

                 [com.taoensso/sente "1.8.1"]
                 [org.clojure/tools.nrepl "0.2.11"]
                 ]
  :min-lein-version "2.7.1"
  :source-paths ["src"]
  :main com.brunobonacci.oneconfig.ui.server
  :plugins [[lein-cljsbuild "1.1.7"]]
  :clean-targets ^{:protect false} [:target-path "resources/public/cljs"]
  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src"]
                        :figwheel     true
                        :compiler     {:main       com.brunobonacci.oneconfig.ui.core
                                       :asset-path "cljs/out"
                                       :output-to  "resources/public/cljs/main.js"
                                       :output-dir "resources/public/cljs/out"}}
                       {:id           "min"
                        :jar          true
                        :source-paths ["src"]
                        :compiler     {:main            com.brunobonacci.oneconfig.ui.core
                                       :output-to       "resources/public/cljs/main.js"
                                       :optimizations   :advanced
                                       :closure-defines {goog.DEBUG false}
                                       :pretty-print    false}}]
              }
  :figwheel {:server-port 5309}
  :profiles {:uberjar {:aot        :all
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]}
             :dev     {:plugins [[lein-figwheel "0.5.15"]]}}
  )

