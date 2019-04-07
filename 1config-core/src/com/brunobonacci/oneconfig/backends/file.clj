(ns com.brunobonacci.oneconfig.backends.file
  (:refer-clojure :exclude [find load list])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer :all]
            [where.core :refer [where]]))


(defn- key->filename
  "convert chars which are not allowed in files names
   with something which doesn't cause problems"
  [s]
  (-> s
     (str/replace #"/" "#")
     (str/replace #"\\" "@")))


(defn- filename->key
  "this function reverses the effect of `key->filename`"
  [s]
  (-> s
     (str/replace #"#" "/")
     (str/replace #"@" "\\\\")))


(defn- search-files
  "searches config files from the base-dir and relative search-root"
  [{:keys [base-dir sep]} search-root]
  (let [search-root (io/file base-dir (str "." sep search-root))
        base-size   (inc (count (.getCanonicalPath ^java.io.File base-dir)))]
    (->>
     ;; list all the files with supported extensions
     (list-files #"(?i).*\.(edn|json|txt|properties)$" search-root
                 :as-string true)
     ;; create relative paths
     (map #(subs % base-size))
     ;; split components
     (map (juxt identity #(str/split % (re-pattern (str "\\Q" sep "\\E")))))
     ;; filter for these with 4 parts
     (filter (where (comp count second) = 4))
     ;; create entries
     (map (fn [[f [k e v t]]]
            {:file (str base-dir sep f)
             :key (filename->key k) :env (filename->key e) :version v
             :content-type (filename->content-type t)
             :change-num (.lastModified (io/file (str base-dir sep f)))})))))



(deftype FileSystemConfigBackend [^java.io.File base-dir sep]

  IConfigClient

  (find [this {:keys [key env version] :as config-entry}]
    (let [key (key->filename key)
          env (key->filename env)
          basefile (str key sep env)
          entries (search-files {:base-dir base-dir :sep sep} basefile)
          entry (->> entries
                   (sort-by (comp comparable-version :version))
                   (take-while #(<= 0 (compare (comparable-version version)
                                               (comparable-version (:version %)))))
                   last)]
      (some-> entry
              (assoc :value (slurp (:file entry)))
              (dissoc :file))))


  IConfigBackend

  (load [_ {:keys [key env version change-num] :as config-entry}]
    (let [key (key->filename key)
          env (key->filename env)
          basefile (str key sep env sep version)
          entry (first (search-files {:base-dir base-dir :sep sep} basefile))]
      (some-> entry
              (assoc :value (slurp (:file entry)))
              (dissoc :file))))



  (save [this {:keys [key env version content-type value] :as config-entry}]
    (let [key (key->filename key)
          env (key->filename env)
          ^java.io.File parent (io/file base-dir key env version)
          _                    (.mkdirs parent)
          file (io/file parent (format "%s.%s" key content-type))]
      (spit file value)
      this))



  (list [this filters]
    (->> (search-files {:base-dir base-dir :sep sep} nil)
       ;; apply post filters and ordering
       (list-entries filters)
       (map #(assoc % :backend :fs)))))



(defn filesystem-config-backend
  ([]
   (filesystem-config-backend (home-1config)))
  ([base-dir]
   (let [dir (io/file base-dir)]
     (when (and (.exists dir) (.isDirectory dir))
       (FileSystemConfigBackend. dir (system-property "file.separator"))))))



(comment
  (list-files #"." (io/file (home-1config)))
  (search-files {:base-dir (io/file (home-1config)) :sep "/"} "/system1/dev")


  (def c (filesystem-config-backend))

  (list c {})
  (list c {:key "system1"})

  (load c {:key "system1" :version "1.3.5" :env "dev"})
  (load c {:key "system1" :version "1.0.0" :env "dev"})

  (find c {:key "system1" :version "1.31.5" :env "dev"})

  (save c
        {:key "system1"
         :env "dev"
         :version "1.6.3"
         :content-type "edn"
         :value (prn-str {:a 1 :b #{:c :d :x}})})

  (find c {:key "system1" :version "1.6.3" :env "dev"})
  (load c {:key "system1" :version "1.6.3" :env "dev"})

  )
