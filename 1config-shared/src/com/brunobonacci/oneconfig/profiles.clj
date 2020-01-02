(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
    com.brunobonacci.oneconfig.profiles
  (:require [com.brunobonacci.oneconfig.util
             :refer [home-1config file-exists? parse-edn deep-merge]]
            [safely.core :refer [safely]]
            [schema.core :as s]
            [where.core :refer [where]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| U S E R - P R O F I L E S |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Example user preferences
;;
(def DEFAULT-PREFERENCES
  {:preferences
   ;; some defaults which can be changed
   { ;; when not specified the default content type is EDN
    :defaults {:content-type "edn"}

    ;; The UI display labels for the various environment
    ;; these are the default colors which can be customized by the user
    :colors
    {:env-labels
     {"local"   "white"
      "dev"     "green"
      "test"    "blue"
      "uat"     "yellow"
      "staging" "yellow"
      "prod"    "red"
      "prd"     "red"
      :default  "grey"}}
    }

   ;; restriction of what can be set in a given account
   ;; good way to set hygiene rules and naming conventions.
   ;; the general form is
   ;;
   ;; [gurad condition] :-> [restriction] :message "user friendly error message"
   :restrictions []
   ;; here some examples
   ;; [:account :matches? ".*"] :-> [:key :matches-exactly? "[a-z][a-z0-9-]+"]
   ;; :message "Invalid service name, shoulbe lowercase letters, numbers and hypens (-)"
   ;;
   ;; [:account :is? "1234567890"] :-> [:env :in? ["prd" "dr-prd"]]
   ;; :message "Invalid environment for this account, only prd, and dr-prd are allowed"
   })



(defn user-profiles
  "loads the user-profiles.edn if present and it merges the content with the defaults."
  []
  (->>
   (some-> (home-1config)
           (str "user-profiles.edn")
           (file-exists?)
           (slurp)
           (parse-edn)
           last)
   (deep-merge DEFAULT-PREFERENCES)))

;; (user-profiles)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;             ----==| U S E R - R E S T R I C T I O N S |==----              ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def ^:private condition1-schema
  [(s/one s/Keyword "field") (s/one s/Keyword "operator") (s/one s/Any "value")])



(def ^:private not-condition1-schema
  [(s/one (s/eq :not) "not") (s/one condition1-schema "cond")])



(def ^:private simple-condition-schema
  (s/either
   [(s/one s/Keyword "field") (s/one s/Keyword "operator") (s/one s/Any "value")]
   [(s/one (s/eq :not) "not") (s/one condition1-schema "cond")]))



(def ^:private condition-schema
  (s/either
   simple-condition-schema
   [(s/one (s/enum :and :or) "logic") condition-schema]))



(def ^:private restriction-schema
  [[(s/one [(s/one s/Keyword "field") (s/one s/Keyword "operator") (s/one s/Any "value")] "guard")
    (s/one (s/eq :->) "arrow")
    (s/one [(s/one s/Keyword "field") (s/one s/Keyword "operator") (s/one s/Any "value")] "restriction")
    (s/one (s/eq :message) ":message")
    (s/one s/Str "error message")]])



(defn- compile-restriction
  [[guard _ restriction _ message]]
  (let [guard* (where guard)
        restriction* (where restriction)]
    (fn [rec]
      ;;(prn "RESTRICTION::" rec :if guard "(" (guard* rec) ")" :-> restriction "("(restriction* rec) ")" :==> (and (guard* rec) (not (restriction* rec))))
      (when (and (guard* rec) (not (restriction* rec)))
        (throw
         (ex-info
          (str "RESTRICTION: " (or message "Invalid values supplied for request."))
          {:data rec
           :guard guard
           :restriction restriction}))))))



(def ^:private restriction-validator
  (s/validator restriction-schema))



(defn- validate-restrictions
  [restrictions]
  (try
    (restriction-validator (partition-all 5 restrictions))
    (catch Exception x
      (throw (ex-info "Invalid restrictions in user-profiles"
                      {:error x :restrictions restrictions})))))



(defn- compile-restrictions
  [restrictions]
  (let [restrictions (validate-restrictions restrictions)
        restrx (map compile-restriction restrictions)]
    (fn [rec]
      (run! (fn [pred] (pred rec)) restrx)
      true)))



(defn apply-restrictions!
  [{:keys [restrictions] :as user-profiles} account request]
  (let [rest-checker (compile-restrictions restrictions)]
    (rest-checker (assoc request :account account))))
