(ns com.brunobonacci.oneconfig.util
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [safely.core :refer [safely]]
            [schema.core :as s]
            [where.core :refer [where]]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream InputStream]
           java.util.zip.GZIPInputStream
           java.util.Properties java.util.Map))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;              ----==| U S E F U L   F U N C T I O N S |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn sem-ver
  "Returns a vector of the numerical components of a 3-leg version number.
  eg:

       (sem-ver \"1.2.3\") ;;=> [1 2 3]
       (sem-ver \"1.2-alpha\") ;;=> nil

  "
  [ver]
  (when ver
    (when-let [components (re-find #"(\d{1,5})\.(\d{1,5})\.(\d{1,5})" ver)]
      (mapv (fn [^String n] (Long/parseLong n)) (rest components)))))



(defn comparable-version
  "takes a 3-leg version number and returns a version string
   which it can be compared lexicographically and maintain it's semantic
   version ordering."
  [ver]
  (apply format "%05d%05d%05d" (sem-ver ver)))



(defn entry-record
  "Given an internal entry, it returns only the keys which are public"
  [entry]
  (when entry
    (select-keys entry [:env :key :version :content-type :value :change-num
                        :user :master-key-alias :master-key])))



(defn- filter-entries [{:keys [key env version]} entries]
  (->> entries
     (filter
      (where [:and
              [:env     :starts-with? (or env "")]
              [:key     :starts-with? (or key "")]
              [:version :starts-with? (or version "")]]))))



(defn list-entries
  [filters entries]
  (let [;; add default ordering to given order
        order (concat (get filters :order-by []) [:key :env (comp comparable-version :version) :change-num])
        ;; use semantic versioning order
        order (map (fn [k] (if (= k :version) (comp comparable-version :version) k)) order)
        ;; compose order function
        order-fn (apply juxt order)]
    (->> entries
       (filter-entries filters)
       (map #(select-keys % [:key :env :version :change-num :content-type
                             :master-key :master-key-alias :user
                             :backend]))
       (sort-by order-fn))))


;; mapcat is not lazy so defining one
(defn lazy-mapcat
  "maps a function over a collection and
   lazily concatenate all the results."
  [f coll]
  (lazy-seq
   (if (not-empty coll)
     (concat
      (f (first coll))
      (lazy-mapcat f (rest coll))))))


(defn clean-map
  "remove keys with nils"
  [map]
  (->> map
     (remove (where second :is? nil))
     (into {})))


(defn nil-value-keys
  "Returns keys which have a nil value"
  [map]
  (->> map
     (filter (where second :is? nil))
     (keys)))


(defmacro show-stacktrace!!
  [show & body]
  {:style/indent 1}
  `(try ~@body
        (catch Throwable x#
          (if ~show
            (.printStackTrace x#)
            (do
              (.println System/err (str "ERROR: " (.getMessage x#)))
              (.println System/err (str "CAUSE: " (loop [e# x#]
                                                    (if (.getCause e#)
                                                      (recur (.getCause e#))
                                                      (.getMessage e#)))))))
          (System/exit 1))))


(defmacro log-configure-request
  "logs the what was requested to configure and it was returned"
  [in & body]
  {:style/indent 1}
  `(let [in# ~in]
     (try
       (let [out# (do ~@body)]
         (log/infof "1config> requested config for: %s received: %s"
                    (pr-str in#)
                    (pr-str (and out#
                               (select-keys out#
                                            [:key :env :version :change-num]))))
         out#)
       (catch Exception x#
         (log/infof "1config> requested config for: %s received: %s"
                    (pr-str in#)
                    (str "ERROR: " (pr-str (.getMessage x#))))
         (throw x#)))))



(defn println-err [& args]
  (binding [*out* *err*]
    (apply println args)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;              ----==| E N V   &   P R O P E R T I E S |==----               ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn env
  "Returns the value of a environment variable or a map with the all variables."
  ([]
   (into {} (System/getenv)))
  ([var]
   (System/getenv var))
  ([var default]
   (or (System/getenv var) default)))



(defn system-property
  "Returns the value of a system property or a map with all properties."
  ([]
   (into {} (System/getProperties)))
  ([property]
   (System/getProperty property)))



(defn oneconfig-version
  []
  (some->
   (io/resource "1config.version")
   slurp
   (str/trim)))



(defn config-property
  [prop-name env-name default]
  (or
   (system-property prop-name)
   (env env-name)
   default))



(defn default-backend-name
  []
  (keyword
   (config-property "1config.default.backend"
                    "ONECONFIG_DEFAULT_BACKEND"
                    "hierarchical")))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| F I L E   U I L I T I E S |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn read-file
  "reads a file and returns its content as a string or nil if not found or can't be read"
  [file]
  (safely
   (slurp file :encoding "utf-8")
   :on-error
   :default nil))



(defn homedir
  "returns the current home dir or nil if not found"
  []
  (or (env "HOME") (env "userprofile")))



(defn home-1config
  "returns ~/.1config/"
  []
  (let [home (homedir)
        dir (some-> home io/file)]
    (when-not (and dir (.exists dir) (.isDirectory dir))
      (log/warn "HOME directory not set or it doesn't exist."))
    (some-> home (str "/.1config/"))))



(defn list-files
  "Returns a lazy list of files and directories for a given path and reg-ex"
  ([pattern dir & {:keys [as-string]
                   :or {as-string false}}]
   (let [matches? (if pattern
                    (fn [^java.io.File f] (re-find pattern (.getCanonicalPath f)))
                    (constantly true))
         mapper   (if as-string (fn [^java.io.File f] (.getCanonicalPath f)) identity)]
     (->> (list-files dir)
          (filter matches?)
          (map mapper))))
  ([dir]
   (if-let [^java.io.File path (and dir (.exists (io/file dir)) (io/file dir))]
     (if (.isDirectory path)
       (lazy-seq
        (cons path
              (mapcat list-files
                      (.listFiles path))))
       [path])
     [])))


(defn file-exists?
  [f]
  (when (and f (.exists (io/file f)))
    f))



(defn configuration-file-search
  "It searches configuration files in a number of different locations.
   it returns a list of entries or nil if no configuration files are found."
  []
  (some file-exists?
        [(system-property "1config.file")
         (env "ONECONFIG_FILE")
         (io/resource "1config.edn")
         (io/resource "1config.json")
         (io/resource "1config.txt")
         (io/resource "1config.properties")
         "./1config.edn"
         "./1config.json"
         "./1config.txt"
         "./1config.properties"]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                  ----==| F I L E   F O R M A T S |==----                   ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn read-edn-file
  "reads a EDN file and returns its content or nil if invalid"
  [file]
  (safely
   (some-> file slurp edn/read-string)
   :on-error :default nil))


(defn read-config-file
  [file]
  (safely
   (some-> file slurp)
   :on-error :default nil))



(defn filename->content-type
  [^String file]
  (->> file
     (re-find #"(?i).*\.(edn|json|txt|properties)$")
     second
     (#(some-> % str/lower-case))))



(defn parse-properties [^String properties]
  (doto (Properties.)
    (.load (ByteArrayInputStream. (.getBytes properties "ISO-8859-1")))))


(defn properties->str [^Properties properties]
  (let [out (java.io.StringWriter.)]
    (.store properties (java.io.PrintWriter. out) nil)
    (-> out (.getBuffer) (.toString))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| E N C O D E / D E C O D E |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmulti decode (fn [form value] form))

(defmethod decode "edn"
  [_ value]
  (edn/read-string value))

(defmethod decode "json"
  [_ value]
  (json/parse-string value true))

(defmethod decode "txt"
  [_ value]
  value)


(defmethod decode "properties"
  [_ value]
  (parse-properties value))


(defmethod decode "props"
  [_ value]
  (decode "properties" value))


(defmulti encode (fn [form value] form))


(defmethod encode "edn"
  [_ value]
  (pr-str value))


(defmethod encode "json"
  [_ value]
  (json/generate-string value))


(defmethod encode "txt"
  [_ value]
  (cond
    (string? value) value
    (number? value) (str value)
    :else
    (throw (ex-info "Illegal value, a string expected" {:value value}))))


(defmethod encode "properties"
  [_ value]
  (cond
    (instance? Properties value) (properties->str value)
    (instance? Map value)        (properties->str (doto (Properties.)
                                                    (.putAll value)))
    :else
    (throw (ex-info "Illegal value, a Properties expected" {:value value}))))


(defmethod encode "props"
  [_ value]
  (encode "properties" value))




(comment
  (defmethod decode "gzip"
    [_ value]
    (let [bout (ByteArrayOutputStream.)
          in  (GZIPInputStream. value)]
      (io/copy in bout)
      (if (instance? InputStream value)
        (.close value))
      (ByteArrayInputStream. (.toByteArray bout)))))


(defn unmarshall-value
  [{:keys [content-type] :as config-entry}]
  (update config-entry :value (partial decode content-type)))


(defn marshall-value
  [{:keys [content-type] :as config-entry}]
  (update config-entry :value (partial encode content-type)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                 ----==| U S E R - P R O F I L E S |==----                  ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn user-profiles
  "loads the user-profiles.edn if present."
  []
  (some-> (home-1config)
          (str "user-profiles.edn")
          (file-exists?)
          (slurp)
          (edn/read-string)))


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
