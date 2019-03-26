(ns com.brunobonacci.oneconfig.backends.file
  (:refer-clojure :exclude [find load list])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.util :refer :all]
            [where.core :refer [where]]))



(defn- search-files [{:keys [base-dir sep]} search-root]
  (let [base-size (inc (count (.getCanonicalPath ^java.io.File base-dir)))]
    (->>
     ;; list all the files with supported extenstions
     (list-files #"(?i).*\.(edn|json|txt|properties)$" search-root :as-string true)
     ;; create relative paths
     (map #(subs % base-size))
     ;; split components
     (map (juxt identity #(str/split % (re-pattern (str "\\Q" sep "\\E")))))
     ;; filter for these with 4 parts
     (filter (where (comp count second) = 4))
     ;; create entries
     (map (fn [[f [k e v t]]]
            {:file (str base-dir sep f)
             :key k :env e :version v
             :content-type (filename->content-type t)
             :change-num (.lastModified (io/file (str base-dir sep f)))})))))



(deftype ReadOnlyFileConfigBackend [^java.io.File base-dir sep]

  IConfigBackend

  (find [this {:keys [key env version] :as config-entry}]
    (let [basefile (str (.getCanonicalPath base-dir)
                        sep key sep env)
          entries (search-files {:base-dir base-dir :sep sep} basefile)
          entry (->> entries
                   (sort-by (comp comparable-version :version))
                   (take-while #(<= 0 (compare (comparable-version version)
                                               (comparable-version (:version %)))))
                   last)]
      (some-> entry
              (assoc :value (slurp (:file entry)))
              (dissoc :file))))



  (load [_ {:keys [key env version change-num] :as config-entry}]
    (let [basefile (str (.getCanonicalPath base-dir)
                        sep key sep env sep version)
          entry (first (search-files {:base-dir base-dir :sep sep} basefile))]
      (some-> entry
              (assoc :value (slurp (:file entry)))
              (dissoc :file))))



  (save [_ config-entry]
    (throw (ex-info "Operation not permitted on this type of backend."
                    {:type "ReadOnlyFileConfigBackend" :base-dir base-dir})))



  (list [this filters]
    (->> (search-files {:base-dir base-dir :sep sep} base-dir)
       ;; apply post filters and ordering
       (list-entries filters))))



(defn readonly-file-config-backend
  ([]
   (readonly-file-config-backend (home-1config)))
  ([base-dir]
   (let [dir (io/file base-dir)]
     (when (and (.exists dir) (.isDirectory dir))
       (ReadOnlyFileConfigBackend. dir (system-property "file.separator"))))))


(comment

  (def c (readonly-file-config-backend))

  (list c {})
  (list c {:key "service1"})

  (load c {:key "service1" :version "1.3.5" :env "dev"})
  (load c {:key "service1" :version "1.2.5" :env "dev"})

  (find c {:key "service1" :version "1.31.5" :env "dev"})
  )
