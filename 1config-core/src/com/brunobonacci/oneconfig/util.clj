(ns com.brunobonacci.oneconfig.util
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [safely.core :refer [safely]]
            [schema.core :as s]
            [where.core :refer [where]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream InputStream]
           java.util.zip.GZIPInputStream))

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


(def config-entry-schema
  {:env     (s/both s/Str (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" ) "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))
   :key     (s/both s/Str (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" ) "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))
   :version (s/pred sem-ver "Version must be of the following form \"1.12.3\"")
   :value   s/Any
   (s/optional-key :content-type)     (s/enum "txt" "edn" "json")
   (s/optional-key :master-key-alias) s/Str
   (s/optional-key :master-key)       s/Str})


(def config-entry-request-schema
  {:env (s/both s/Str (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" ) "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))
   :key (s/both s/Str (s/pred (partial re-matches #"^[a-zA-Z0-9/_-]+$" ) "Must match the following pattern: ^[a-zA-Z0-9/_-]+$"))
   :version (s/pred sem-ver "Version must be of the following form \"1.12.3\"")
   (s/optional-key :change-num) s/Int})


(defn check-entry [entry]
  (s/check config-entry-schema entry))


(defn valid-entry? [entry]
  (s/validate config-entry-schema entry))


(defn valid-entry-request? [entry]
  (s/validate config-entry-request-schema entry))


(defn comparable-version
  "takes a 3-leg version number and returns a version string
   which it can be compared lexicographically and maintain it's semantic
   version ordering."
  [ver]
  (apply format "%05d%05d%05d" (sem-ver ver)))


(defn env
  "Returns the value of a environment variable or a map with the all variables."
  ([]
   (into {} (System/getenv)))
  ([var]
   (System/getenv var)))



(defn system-property
  "Returns the value of a system property or a map with all properties."
  ([]
   (into {} (System/getProperties)))
  ([property]
   (System/getProperty property)))



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
  (env "HOME"))



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



(defn search-candidates
  "given a list of candidates searches for configuration files."
  [& candidates]
  (filter identity (flatten candidates)))



(defn read-edn-file
  "reads a EDN file and returns its content or nil if invalid"
  [file]
  (safely
   (some-> file slurp edn/read-string)
   :on-error :default nil))


(defn entry-record
  "Given an internal entry, it returns only the keys which are public"
  [entry]
  (when entry
    (select-keys entry [:env :key :version :content-type :value :change-num])))



(defn configuration-file-search
  "It searches configuration files in a number of different locations.
   it returns a list of entries or nil if no configuration files are found."
  []
  (some (fn [f] (and (.exists (io/file f)) f))
        (search-candidates
         (env "ONECONFIG_FILE")
         (system-property "1config.file")
         (io/resource "1config.edn")
         "./1config/1config.edn"
         (str (homedir) "/.1config/1config.edn"))))



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


(defn- filter-entries [{:keys [key env version]} entries]
  (->> entries
     (filter
      (where [:and
              [:env :starts-with? (or env "")]
              [:key :starts-with? (or key "")]
              [:version :starts-with? (or version "")]]))))


(defn list-entries [filters entries]
  (->> entries
       (filter-entries filters)
       (sort-by (juxt :key :env :version :change-num))
       (map #(dissoc % :__ver_key :__sys_key :value :zver))))


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
