(ns com.brunobonacci.oneconfig.main
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [com.brunobonacci.oneconfig.cli :as cli]
            [com.brunobonacci.oneconfig.util :as util]
            [com.brunobonacci.oneconfig.backends :as b]
            [safely.core :refer [safely]]
            [clojure.java.io :as io]))

(def ^:dynamic *repl-session* false)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                           ---==| M A I N |==----                           ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn help! [errors]
  (println
   (format "
      1config cli
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                     v%s

  A command line tool to manage application secrets and configuration safely and effectively.

Usage:

   1cfg <OPERATION> -e <ENVIRONMENT> -k <SERVICE> [-v <VERSION>] [-b <BACKEND>] [-t <TYPE>] <VALUE>

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
        --stacktrace           : To show the full stacktrace of an error
   -b   --backend   BACKEND    : Must be one of: hierarchical, dynamo, fs. Default: hierarchical
   -e   --env   ENVIRONMENT    : the name of the environment like 'prod', 'dev', 'st1' etc
   -k   --key       SERVICE    : the name of the system or key for which the configuration if for,
                               : exmaple: 'service1', 'db-pass' etc
   -v   --version   VERSION    : a version number for the given key in the following format: '2.12.4'
                               : If not provided, the latest version will be returned.
   -c   --change-num CHANGENUM : used with GET returns a specific configuration change.
   -f   --content-file FILE    : read the value to SET from the given file.
        --with-meta            : whether to include meta data for GET operation
        --output-format FORMAT : either 'table' or 'cli' default is 'table' (only for list)
   -C                          : same as '--output-format=cli'
   -X   --extented             : whether to display an extended table (more columns)
   -P   --pretty-print         : whether to pretty print the configuration values
   -o   --order-by     ORDER   : The listing order, must be a comma-separated list
                               : of one or more of: 'key', 'env', 'version', 'change-num'
                               : default order: 'key,env,version,change-num'
   -t   --content-type TYPE    : one of 'edn', 'txt' or 'json', 'properties' or 'props'
                               : default is 'edn'
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

   (*) To initialise a given backend (first time only)
   1cfg INIT -b dynamo

   (*) To set the configuration value of a service called 'service1' use:
   1cfg SET -b dynamo -e test -k 'service1' -v '1.6.0' -t edn '{:port 8080}'

   (*) To read last configuration value for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1'

   (*) To read last change for a specific version of 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0'

   (*) To read a specific changeset for a service called 'service1' use:
   1cfg GET -b dynamo -e test -k 'service1' -v '1.6.0' -c '3563412132'

   (*) To list configuration with optional filters and ordering
   1cfg LIST -b dynamo -e prod -k ser -v 1. -o env,key


NOTE: set AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY or AWS_PROFILE to
      provide authentication access to the target AWS account.
      set AWS_DEFAULT_REGION to set the AWS region to use.

" (util/oneconfig-version))
   (str/join "\n" errors))
  (System/exit 1))


(defn normal-exit!
  []
  (when-not *repl-session*
    (shutdown-agents)
    (System/exit 0)))


(def cli-options
  [["-h"  "--help"]
   [nil  "--stacktrace"]

   ["-b"  "--backend BACKEND"
    :validate [#{"hierarchical" "dynamo" "fs"} "Must be one of: hierarchical, dynamo, fs"]]


   [nil "--with-meta"]

   [nil "--output-format OUTPUT"
    :parse-fn keyword
    :validate [#{:table :cli}]
    :default :table]

   ["-C" "--cli-format"]
   ["-P" "--pretty-print"]
   ["-X" "--extended"]

   ["-e"  "--env ENV"]

   ["-k"  "--key KEY"]

   ["-v"  "--version VER"]

   ["-c"  "--change-num CHANGENUM"
    :parse-fn (fn [num-str] (when num-str (Long/parseLong num-str)))]

   ["-t"  "--content-type TYPE"
    :default "edn"
    :validate [#{"edn" "txt" "json" "properties" "props"}
               "Must be one of: edn, txt, json, properties, props"]]

   ["-f"  "--content-file FILENAME"
    :parse-fn io/file
    :validate [#(.exists ^java.io.File %) "The file must exist"]]

   ["-o"  "--order-by ORDER"
    :default [:key :env :version :change-num]
    :parse-fn #(-> % (str/split #" *, *") ((partial map keyword)))
    :validate [(partial every? #{:change-num :key :env :version})
               "Must be a comma-separated list of: key, env, version, change-num"]]

   ["-m"  "--master-key KEY-NAME"]])



(defn- nil-argument-names [hm]
  (->> hm
     (util/nil-value-keys)
     (map name)
     (str/join ", ")))



(defn- validate-format
  "Returns `value` if the format validation is successful according to
  the `content-type`, otherwise exit(2)."
  [content-type value]

  ;; parsing value to check if content is valid
  (safely
   (util/decode content-type value)
   :on-error
   :log-stacktrace false
   :message (str "Parsing value as " content-type))

  ;; returning the original value to be stored
  ;; this is to preserve comments
  value)



(defn -main [& args]
  (let [{:keys [options arguments errors] :as cli} (parse-opts args cli-options)
        {:keys [help backend env key version change-num
                content-type with-meta output-format order-by
                master-key extended pretty-print content-file]} options
        [op value & too-many-args] arguments
        op (when op (keyword (str/lower-case op)))
        backend-name (or (keyword backend) (util/default-backend-name))
        output-format (if (:cli-format options) :cli output-format)
        ;; if table and extended show :tablex
        output-format (or (and (= output-format :table) extended :tablex) output-format)]

    (cond
      help            (help! [])
      errors          (help! errors)
      (not op)        (help! ["MISSING: required argument: operation"])

      ;; check for invalid operation
      (not
       (#{:get :set :init :list :list-keys :create-key} op))
      (help! ["INVALID operation: must be either GET, SET, LIST, INIT, LIST-KEYS or CREATE-KEY"])

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
                              :value (validate-format content-type
                                                      (or value (some-> content-file slurp)))} $
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
                           :extended extended))
        (normal-exit!)))))
