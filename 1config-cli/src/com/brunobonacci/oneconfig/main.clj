(ns com.brunobonacci.oneconfig.main
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [com.brunobonacci.oneconfig.cli :as cli]
            [com.brunobonacci.oneconfig.util :as util]
            [com.brunobonacci.oneconfig.backends :as b]
            [safely.core :refer [safely]]
            [clojure.java.io :as io]
            ;; added for GraalVM
            [cognitect.aws.protocols.json]
            [cognitect.aws.protocols.common]
            [cognitect.aws.protocols.query]
            [cognitect.http-client]
            [clojure.spec.alpha]))



(def ^:dynamic *repl-session* false)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                           ---==| M A I N |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;                       ----==| 1 C O N F I G |==----                        ;;




(defn help! [errors]
  (println
    (format (str (slurp (io/resource "help-page.txt")) \newline)
      (util/oneconfig-version))
    (str/join "\n" errors))
  (System/exit 0))



(defn normal-exit!
  []
  (when-not *repl-session*
    (shutdown-agents)
    (System/exit 0)))



(def cli-options
  (let [repeated-key* (fn [m k v] (update m k #(conj (or % []) v)))]
    [["-h"  "--help"]
     [nil  "--stacktrace"]

     ["-b"  "--backend BACKEND"
      :assoc-fn repeated-key*
      :validate [#{"hierarchical" "dynamo" "fs"} "Must be one of: hierarchical, dynamo, fs"]]


     [nil "--with-meta"]

     [nil "--output-format OUTPUT"
      :parse-fn keyword
      :validate [#{:table :cli}]
      :default :table]

     ["-D" "--diff-mode MODE"
      :parse-fn keyword
      :validate [#{:line :char}]
      :default :line]

     [nil "--DC"
      :assoc-fn (fn [m k v] (assoc m :diff-mode :char))]

     ["-C" "--cli-format"]
     ["-P" "--pretty-print"]
     ["-X" "--extended"]

     ["-e"  "--env ENV" :assoc-fn repeated-key*]

     ["-k"  "--key KEY" :assoc-fn repeated-key*]

     ["-v"  "--version VER" :assoc-fn repeated-key*]

     ["-c"  "--change-num CHANGENUM"
      :assoc-fn repeated-key*
      :parse-fn (fn [num-str] (when num-str (Long/parseLong num-str)))]

     ["-t"  "--content-type TYPE"
      :default "edn"
      :validate [#{"edn" "txt" "json" "properties" "props", "yaml"}
                 "Must be one of: edn, txt, json, yaml, properties, props"]]

     ["-f"  "--content-file FILENAME"
      :parse-fn io/file
      :validate [#(.exists ^java.io.File %) "The file must exist"]]

     ["-o"  "--order-by ORDER"
      :default [:key :env :version :change-num]
      :parse-fn #(-> % (str/split #" *, *") ((partial map keyword)))
      :validate [(partial every? #{:change-num :key :env :version})
                 "Must be a comma-separated list of: key, env, version, change-num"]]

     ["-m"  "--master-key KEY-NAME"]]))



(defn- nil-argument-names [hm]
  (->> hm
    (util/nil-value-keys)
    (map name)
    (str/join ", ")))



(defn- multi-key
  [pos params]
  (let [pos* (if (= 0 pos) first #(or (second %) (first %)))]
    (map pos* params)))



(defn -main [& args]
  (let [{:keys [options arguments errors] :as cli} (parse-opts args cli-options)
        {:keys [help backend env key version change-num
                content-type with-meta output-format order-by
                master-key extended pretty-print content-file
                diff-mode]} options
        [op value & too-many-args] arguments
        op (when op (keyword (str/lower-case op)))

        [backend2 env2 key2 version2 change-num2] (multi-key 1 [backend env key version change-num])
        [backend  env  key  version  change-num]  (multi-key 0 [backend env key version change-num])
        backend-name (or (keyword backend) (util/default-backend-name))
        backend-name2 (or (keyword backend2) (util/default-backend-name))
        output-format (if (:cli-format options) :cli output-format)
        ;; if table and extended show :tablex
        output-format (or (and (= output-format :table) extended :tablex) output-format)
        ]

    (cond
      help            (help! [])
      errors          (help! errors)
      (not op)        (help! ["MISSING: required argument: operation"])

      ;; check for invalid operation
      (not
        (#{:get :set :init :list :diff :list-keys :create-key} op))
      (help! ["INVALID operation: must be either GET, SET, LIST, INIT, DIFF, LIST-KEYS or CREATE-KEY"])

      ;; check for missing value on set
      (and (= op :set) (not (or value content-file)))
      (help! ["MISSING: required argument: value, provide a value in-line or via the '-f filename' option"])

      ;; check whether two values have been provided
      (and value content-file)
      (help! [(format "TWO VALUES PROVIDED: inline and -f %s" content-file)])

      ;; check for missing required args on get and set
      (and (= op :set) (some nil? [env key version]))
      (help! [(str "MISSING: required arguments: "
                (nil-argument-names {:env env, :key key, :version version}))])

      (and (= op :get) (some nil? [env key]))
      (help! [(str "MISSING: required arguments: "
                (nil-argument-names {:env env, :key key}))])

      ;; check for missing key name on create-key
      (and (= op :create-key) (not master-key))
      (help! ["MISSING: required argument: master-key"])

      ;; check for trailing unaccounted arguments
      (seq too-many-args)
      (help! [(str "TOO MANY ARGUMENTS: " (str/join ", " too-many-args))])

      ;; else, is a valid request
      :else
      (util/show-stacktrace!! (:stacktrace options)
        (case op
          ;;
          ;; LIST-KEYS
          ;;
          :list-keys (cli/list-keys!)

          ;;
          ;; CREATE-KEY
          ;;
          :create-key (cli/create-key! {:key-name master-key})

          ;;
          ;; INIT
          ;;
          :init (cli/init-backend! backend-name)

          ;;
          ;; SET
          ;;
          :set (cli/set! (keyword (or backend-name (util/default-backend-name)))
                 (b/backend-factory {:type backend-name :force true})
                 (as->
                  {:env env :key key :version version
                   :content-type content-type
                   :value (or value (some-> content-file slurp))} $
                   (if master-key (assoc $ :master-key master-key) $)))

          ;;
          ;; GET
          ;;
          :get (cli/get! (b/backend-factory {:type backend-name})
                 {:env env :key key :version version :change-num change-num}
                 :with-meta with-meta :pretty-print? pretty-print)

          ;;
          ;; LIST
          ;;
          :list (cli/list! (b/backend-factory {:type backend-name})
                  {:env env :key key :version version :order-by order-by}
                  :output-format output-format :backend-name backend-name
                  :extended extended)

          ;;
          ;; DIFF
          ;;
          :diff (cli/diff! [(b/backend-factory {:type backend-name})
                            {:env env :key key :version version :change-num change-num}]
                  [(b/backend-factory {:type backend-name})
                   {:env env2 :key key2 :version version2 :change-num change-num2}]
                  :mode diff-mode)
          )
        (normal-exit!)))))
