(ns com.brunobonacci.oneconfig.main
  (:require [com.brunobonacci.oneconfig.cli :as cli]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str])
  (:gen-class))


(def ^:dynamic *repl-session* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                           ---==| M A I N |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn help! [errors]
  (println "
      1config cli
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

  A command line tool for managing configurations in different environments.

Usage:

   1cfg <OPERATION> -e <ENVIRONMENT> -k <SERVICE> -v <VERSION> [-b <BACKEND>] [-t <TYPE>] <VALUE>

   WHERE:
   ---------

   OPERATION:
      - GET        : retrieve the current configuration value for
                   : the given env/service/version combination
      - SET        : sets the value of the given env/service/version combination
      - LIST       : lists the available keys for the given backend
      - INIT       : initialises the given backend (like create the table if necessary)
      - LIST-KEYS  : lists the master encryption keys created by 1Config.
      - CREATE-KEY : creates an master encryption key.

   OPTIONS:
   ---------
   -h   --help                 : this help
   -b   --backend   BACKEND    : only 'dynamo' is currently supported, and it is the default one.
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db.pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
        --with-meta            : whether to include meta data for GET operation
        --output-format FORMAT : either 'table' or 'cli' default is 'table' (only for list)
   -C                          : same as '--output-format=cli'
   -o   --order-by     ORDER   : The listing order, must be a comma-separated list
                               : of one or more of: 'key', 'env', 'version', 'change-num'
                               : default order: 'key,env,version,change-num'
   -t   --content-type TYPE    : one of 'edn', 'txt' or 'json', default is 'edn'
   -m   --master-key  KEY-NAME : The master encryption key to use for encrypting the entry.
                               : It must be a KMS key alias or an arn identifier for a key.

Example:

   --- keys management ---

   (*) List KMS encryption keys managed by 1Config
   1cfg LIST-KEYS

   (*) Create a master encryption key, the key name must be the same
       and the configuration key to be used automatically.
   1cfg CREATE-KEY -m 'service1'

   --- configuration entries management  ---

   (*) To initialise a given backend
   1cfg INIT -b dynamo

   (*) To set the configuration value of a service called 'service1' use:
   1cfg SET -b dynamo -e test -k 'service1' -v '1.6.0' -t edn '{:port 8080}'

   (*) To read last configuration value for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0'

   (*) To read a specific changeset for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0' -c '3563412132'

   (*) To list configuration with optional filters and ordering
   1cfg LIST -b dynamo -e prod -k ser -v 1. -o env,key


NOTE: set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to
      provide authentication access to the target AWS account.
      set AWS_DEFAULT_REGION to set the AWS region to use.

" (str/join "\n" errors))
  (System/exit 1))


(defn normal-exit!
  []
  (when-not *repl-session*
    (shutdown-agents)
    (System/exit 0)))


(def cli-options
  [["-h"  "--help"]

   ["-b"  "--backend BACKEND"
    :default "dynamo"
    :validate [(partial re-find #"(?i)^(dynamo)$") "Must be one of: dynamo"]]


   [nil "--with-meta"]

   [nil "--output-format OUTPUT"
    :parse-fn keyword
    :validate [#{:table :cli}]
    :default :table]

   ["-C" "--C"]

   ["-e"  "--env ENV"]

   ["-k"  "--key KEY"]

   ["-v"  "--version VER"
    :default ""]

   ["-c"  "--change-num CHANGENUM"
    :parse-fn (fn [num-str] (when num-str (Long/parseLong num-str)))]

   ["-t"  "--content-type TYPE"
    :default "edn"
    :validate [#{"edn" "txt" "json"} "Must be one of: edn, txt, json"]]

   ["-o"  "--order-by ORDER"
    :default [:key :env :version :change-num]
    :parse-fn #(-> % (str/split #" *, *") ((partial map keyword)))
    :validate [(partial every? #{:change-num :key :env :version})
               "Must be a comma-separated list of: key, env, version, change-num"]]

   ["-m"  "--master-key KEY-NAME"]])


(defn -main [& args]
  (let [{:keys [options arguments errors] :as cli} (parse-opts args cli-options)
        {:keys [help backend env key version change-num
                content-type with-meta output-format order-by
                master-key]} options
        [op value] arguments
        op (when op (keyword (str/lower-case op)))
        backend-name (keyword backend)
        output-format (if (:C options) :cli output-format)]

    (cond
      help            (help! [])
      errors          (help! errors)
      (not op)        (help! ["MISSING: required argument: operation"])
      (not
       (#{:get :set :init :list :list-keys :create-key} op)) (help! ["INVALID operation: must be either GET, SET, LIST, INIT, LIST-KEYS or CREATE-KEY"])
      (and (= op :set) (not value)) (help! ["MISSING: required argument: value"])
      (and (= op :create-key) (not master-key)) (help! ["MISSING: required argument: master-key"])

      :else
      (do
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
          :set (cli/set! (cli/backend backend-name)
                         {:env env :key key :content-type content-type
                          :version version :value
                          (or (cli/decode content-type value) (System/exit 2))})

          ;;
          ;; GET
          ;;
          :get (cli/get! (cli/backend backend-name)
                         {:env env :key key :version version :change-num change-num}
                         :with-meta with-meta)

          ;;
          ;; LIST
          ;;
          :list (cli/list! (cli/backend backend-name)
                           {:env env :key key :version version :order-by order-by}
                           :output-format output-format :backend-name backend-name))
        (normal-exit!)))))
