(ns com.brunobonacci.oneconfig.table
  (:require [clojure.string :as str]))



(defn left
  [sz s]
  (format (str "%-" sz "s") (or s "")))



(defn right
  [sz s]
  (format (str "%" sz "s") (or s "")))



(defn center
  [sz s]
  (if (< (count s) sz)
    (let [hf (+ (quot (count s) 2) (quot sz 2))]
      (left sz (right hf s)))
    s))



(defn line
  [sz]
  (apply str (repeat sz "-")))



(defn align-to
  [al sz s]
  (case al
    nil     (left   sz s)
    :left   (left   sz s)
    :center (center sz s)
    :right  (right  sz s)))



(defn sep-line
  [table-def]
  (->> table-def
    (map (fn [{:keys [size]}] (line size)))
    (str/join "-+-")
    (format "+-%s-+")))



(defn content-line
  [table-def cnt]
  (->> table-def
    (map (fn [{:keys [align size name]}]
           (align-to align size (get cnt name))))
    (str/join " | ")
    (format "| %s |")))



(defn normalize-header
  [table-def]
  (map (fn [{:keys [title name] :as m}] (assoc m :title (str (or title name)))) table-def))



(defn header-line
  [table-def]
  (as-> table-def $
    (normalize-header $)
    (content-line
      (map #(assoc % :align :center) $)
      (into {} (map (fn [{:keys [title name] :as m}] [name title]) $)))))



(defn compute-sizes
  [table-def data]
  (let [max-size (fn [f min data] (->> data (map f) (remove nil?) (map (comp count str)) (reduce max min)))]
    (->> table-def
      (map (fn [{:keys [name title] :as m}]
             (update m :size (fn [ov] (or ov (max-size #(get % name) (count title) data)))))))))



(defn format-record
  [table-def]
  (fn [record]
    (->> table-def
      (map (fn [{:keys [name format]}]
             (let [format (or format identity)]
               [name (str (format (get record name)))])))
      (into {}))))



(defn format-data
  [table-def data]
  (let [fmt (format-record table-def)]
    (map fmt data)))



(defn table-lines
  [table-def data]
  (let [table-def (normalize-header table-def)
        data      (format-data table-def data)
        table-def (compute-sizes table-def data)]
    (concat
      [(sep-line table-def)
       (header-line table-def)
       (sep-line table-def)]
      (map (partial content-line table-def) data)
      [(sep-line table-def)])))



(defn table-str
  [table-def data]
  (->> (table-lines table-def data)
    (str/join "\n")))



(defn table
  [table-def data]
  (println (table-str table-def data)))
