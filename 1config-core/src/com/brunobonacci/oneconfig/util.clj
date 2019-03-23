(ns com.brunobonacci.oneconfig.util
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [safely.core :refer [safely]]
            [schema.core :as s]
            [where.core :refer [where]])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream InputStream]
           java.util.zip.GZIPInputStream
           java.util.Properties java.util.Map))

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



(defn parse-properties [^String properties]
  (doto (Properties.)
    (.load (ByteArrayInputStream. (.getBytes properties "ISO-8859-1")))))


(defn properties->str [^Properties properties]
  (let [out (ByteArrayOutputStream.)]
    (.store properties out nil)
    (String. (.toByteArray out) "ISO-8859-1")))



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



(defn- filter-entries [{:keys [key env version]} entries]
  (->> entries
     (filter
      (where [:and
              [:env :starts-with? (or env "")]
              [:key :starts-with? (or key "")]
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
                             :master-key :master-key-alias]))
       (sort-by order-fn))))


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


(defn clean-map
  "remove keys with nils"
  [map]
  (->> map
     (remove (where second :is? nil))
     (into {})))


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
