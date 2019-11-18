(defn ver [] (-> "../ver/1config.version" slurp .trim))
(defproject com.brunobonacci/oneconfig-ui #=(ver)

  :jvm-opts ~(let [version (System/getProperty "java.version")
                   [major _ _] (clojure.string/split version #"\.")]
               (if (>= (java.lang.Integer/parseInt major) 9)
                 ["--add-modules" "java.xml.bind"]
                 []))

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [com.brunobonacci/oneconfig #=(ver)]
                 [figwheel-sidecar "0.5.18"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljs-ajax "0.8.0"]
                 [cljsjs/react "16.9.0-0"]
                 [cljsjs/react-dom "16.9.0-0"]

                 [cheshire "5.9.0"]

                 [http-kit "2.3.0"]
                 [compojure "1.6.1"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [ring/ring-json "0.5.0"]
                 [ring-cors "0.1.13"]
                 [enlive "1.1.6"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.9"]

                 [re-frisk "0.5.4.1"]
                 [com.taoensso/sente "1.13.1"]              ;; http kit

                 [org.slf4j/slf4j-log4j12 "1.7.26"]
                 ]
  :min-lein-version "2.7.1"
  :source-paths ["src"]
  :main com.brunobonacci.oneconfig.ui.server

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

  :plugins [[lein-cljsbuild "1.1.7"]]
  :profiles {:uberjar {:aot        :all
                       :prep-tasks ["compile" ["cljsbuild" "once" "min"]]}
             :dev     {:plugins [[lein-figwheel "0.5.19"]]}}
  )
