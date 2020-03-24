(ns com.brunobonacci.oneconfig.ui.controller
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as reagent :refer [atom]]
   [cljs.core.async :as async :refer [<! put! chan]]
   [ajax.core :refer [GET POST]]
   [cljs.pprint :as pp]
   [com.brunobonacci.oneconfig.ui.utils :as utils]
   [goog.string :as gs]))


(enable-console-print!)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                         ----==| S T A T E |==----                          ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def empty-new-entry
  {:key       ""
   :env       ""
   :version   ""
   :type      ""
   :val       ""
   :file      ""
   :file-name ""
   })

(def empty-selected {:counter 0 :items-meta [] :entries {:left nil :right nil}})

(def ace-theme-mapping {"json" "json", "txt"  "text", "edn" "clojure", "properties" "properties"})

(defonce state
  (atom
   {
    ;; user defined preferences
    :preferences nil
    ;; contains the config entries retrieved from the server
    :entries []
    ;; filters
    :filters {:key "", :env "", :version ""}
    ;; manages the toggle for the extended mode
    :extended-mode? false
    ;; one of :listing, :new-entry-mode, :show-entry-mode, :edit-entry-mode
    :client-mode :listing
     ;:new-version-flag? nil
    :1config-version {:current "" :latest ""}

    :new-entry empty-new-entry

    :entry-management-button-style "ui inverted button"

    ;; modal window (initial as plain, should be nested) it should be  ":show-entry-mode"
    :item-data   nil
    :item-params nil
    :selected empty-selected
    }))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ----==| H A N D L E R S |==----                       ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn error-handler [{:keys [status status-text]}]
  (pp/pprint (str "failure occurred: " status " " status-text)))

(defn backend-error-handler
  [{:keys [status status-text parse-error]}]
  (js/alert (str " Operation failed \n status: " status "\n message: " status-text  "\n details: " (:original-text parse-error))))

(defn error-add-config-handler
  [{:keys [response]}]
  (js/alert (str " Operation failed \n status: " (:status response) "\n cause: " (:cause response))))

(defn update-version! [version]
  (swap! state assoc :1config-version version))


(defn GETAsync [url]
  (let [out (chan)
        handler #(put! out %1)]
    (GET url {:handler handler
              :format          :json
              :response-format :json
              :keywords?       true
              :error-handler handler})
    out))

(defn create-url-a [item]
  (let [{:keys [key env version change-num]} item]
    (gs/format "/configs/keys/%s/envs/%s/versions/%s?change-num=%s"
               (gs/urlEncode key) (gs/urlEncode env)
               (gs/urlEncode version) change-num)))

(defn compare-selected-items [compare-items]
  (go
    (let [urls (map #(create-url-a %) (get compare-items :items-meta))
          configs [(<! (GETAsync (first urls)))
                   (<! (GETAsync (last urls)))]
          selected-entries (map #(:encoded-value %) configs)]
      (swap! state #(-> %
                        (assoc-in [:new-entry] empty-new-entry)
                        (assoc-in [:selected :counter] 0)
                        (assoc-in [:selected :entries :left]  (first selected-entries))
                        (assoc-in [:selected :entries :right]  (last selected-entries))
                        (assoc-in [:entry-management-button-style] "ui inverted disabled button")
                        (assoc-in [:client-mode] :compare-entry-mode))))))

(defn row-selected
  [e item]
  (let [parent (-> e .-target .-parentElement .-parentElement)]
    (if (.. e -target -checked)
      (do
        (swap! state update-in [:selected :counter] inc)
        (swap! state update-in [:selected :items-meta]  merge item)
        (.add (.-classList parent) "selected"))
      (do
        (swap! state update-in [:selected :counter] dec)
        (let [change-num  (:change-num item)
              items-meta (get-in @state [:selected :items-meta] )
              new-items-meta (remove #(= change-num (:change-num %)) items-meta)]
          (println "New items-meta : " new-items-meta)
          (swap! state assoc-in [:selected :items-meta]  new-items-meta)
          )
        (.remove (.-classList parent) "selected")
        ))))

(defn all-rows-selected
  [e]
  (println "To be done"))

(defn get-item-handler [response]
  (swap! state
         (fn [s]
           (-> s
               (assoc :item-data (get response :encoded-value))
               (assoc :client-mode :show-entry-mode)))))

(defn footer-element
  [version]
  (str "1Config - A library to manage application secrets "
       "and configuration safely and effectively.  "
       "Apache License 2.0. Bruno Bonacci, 2019-2020, v." (get version :current)
       (if (= (get version :current) (get version :latest))
         ""
         (str "  (Latest version v." (get version :latest) ")"))))


(defn get-preferences! []
  (GET "/preferences"
       {
        :handler         (fn [prefs]
                           (swap! state assoc :preferences prefs))
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))


(defn all-configs-handler!
  [entries]
  (swap! state
         (fn [s]
           (-> s
               (assoc-in [:new-entry] empty-new-entry)
               (assoc :entries entries)
               (assoc :client-mode :listing)))))


(defn get-all-configs! []
  (GET "/configs"
       {
        :handler         all-configs-handler!
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   backend-error-handler}))


(defn get-config-item! [item]
  (let [{:keys [key env version change-num content-type]} item
        get-url (gs/format "/configs/keys/%s/envs/%s/versions/%s?change-num=%s"
                           (gs/urlEncode key) (gs/urlEncode env)
                           (gs/urlEncode version) change-num)]

    (swap! state assoc-in [:item-params] {:key          key
                                          :env          env
                                          :version      version
                                          :change-num   change-num
                                          :content-type content-type})

    (GET get-url {:handler         get-item-handler
                  :format          :json
                  :response-format :json
                  :keywords?       true
                  :error-handler   backend-error-handler})))

(defn add-config-entry! [event]
  (.preventDefault event)
  (let [new-entry (get @state :new-entry)
        ace-instance (.edit js/ace "jsEditor")
        form-data (doto
                      (js/FormData.)
                    (.append "key"          (get new-entry :key))
                    (.append "env"          (get new-entry :env))
                    (.append "version"      (get new-entry :version))
                    (.append "content-type" (if (empty? (get new-entry :type)) "json" (get new-entry :type)))
                    (.append "value"         (.getValue ace-instance)))]

    (swap! state assoc-in [:entry-management-button-style] "ui inverted button")
    (enable-body-scroll)
    (POST "/configs" {:body            form-data
                      :response-format :json
                      :keywords?       true
                      :handler         get-all-configs!
                      :error-handler   error-add-config-handler})))



(defn get-version! []
  (GET "/info/versions"
       {:handler          update-version!
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;            ----==| S T A T E   M A N I P U L A T I O N |==----             ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn remove-file! []
  (swap! state
         #(-> %
              (assoc-in [:new-entry :val] "")
              (assoc-in [:new-entry :file-name] ""))))

(defn config-entry-management-panel!
  [mode]
  (cond
    (= :listing mode)         (swap! state #(-> %
                                                (assoc-in [:entry-management-button-style] "ui inverted disabled button")
                                                (assoc-in [:client-mode] :new-entry-mode)))
    (= :new-entry-mode mode)  (swap! state assoc-in [:client-mode] :edit-entry-mode)
    (= :show-entry-mode mode) (swap! state #(-> %
                                              (assoc-in [:new-entry] empty-new-entry)
                                              (assoc-in [:entry-management-button-style] "ui inverted disabled button")
                                              (assoc-in [:client-mode] :edit-entry-mode)))
    (= :edit-entry-mode mode) (swap! state #(-> %
                                              (assoc-in [:new-entry] empty-new-entry)
                                              (assoc-in [:client-mode] :new-entry-mode)))
    :else (println (str "unknown mode : " mode))))

(defn copy-data-to-new-entry!
  [item-params item-data]
  (swap! state assoc-in [:new-entry] {:key       (get item-params :key)
                                      :env       (get item-params :env)
                                      :version   (get item-params :version)
                                      :type      (get item-params :content-type)
                                      :val       item-data
                                      :file      ""
                                      :file-name ""
                                      }))

(defn toggle-table-mode!
  []
  (swap! state update-in [:extended-mode?] not))

(defn discard-changes!
  [val]
  (let [ace-instance (.edit js/ace "jsEditor")]
    (.setValue ace-instance val)))

(defn close-new-entry-panel!
  [event]
  (.preventDefault event)
  (enable-body-scroll)
  (swap! state #(-> %
                    (assoc-in [:item-params] nil)
                    (assoc-in [:item-data] nil)
                    (assoc-in [:new-entry] empty-new-entry)
                    (assoc-in [:entry-management-button-style] "ui inverted button")
                    (assoc-in [:client-mode] :listing))))

(defn nodelist-to-seq
  [nl]
  (let [result-seq (map #(.item nl %) (range (.-length nl)))]
    (doall result-seq)))

(defn uncheck-box
  [box]
  (set! (.-checked box) false))

(defn close-compare-entries-panel!
  [event]
  (.preventDefault event)
  (enable-body-scroll)
  (let [selected-lines (apply list (-> js/document
                                       (.getElementsByClassName "selected")
                                       array-seq))]
    (doall (map #(.remove (.-classList %) "selected") selected-lines))
    (doall (map #(-> % .-childNodes
                       nodelist-to-seq
                       last
                       .-childNodes
                       nodelist-to-seq
                       last
                       uncheck-box) selected-lines)))
  (swap! state #(-> %
                    (assoc-in [:selected] empty-selected)
                    (assoc-in [:entry-management-button-style] "ui inverted button")
                    (assoc-in [:client-mode] :listing))))

;;https://github.com/search?l=Clojure&p=2&q=.execCommand+js%2Fdocument&type=Code
(defn copy-to-clipboard!
  []
  (let [e (. js/document (createElement "textarea"))
        ace-instance (.edit js/ace "jsEditor")]
    (.. js/document -body (appendChild e))
    (set! (.-value e) (.getValue ace-instance))
    (.select e)
    (.setSelectionRange e 0 99999)
    (. js/document (execCommand "copy"))
    (.. js/document -body (removeChild e)))
  (js/alert "Copied to clipboard"))

(defn highlight-ace-code-block!
  [editable? type]
  (disable-body-scroll)
  (let [ace-instance (.edit js/ace "jsEditor")]
    (.setTheme ace-instance "ace/theme/github")
    (.setMode (.getSession ace-instance) (gs/format "ace/mode/%s" type))
    (.setUseWorker (.getSession ace-instance) false)
    (.setReadOnly ace-instance editable?)
    (.setHighlightActiveLine ace-instance true)))

(defn compare-ace-code-block!
  [left right]
  (let [left (js-obj "content" left)
        right (js-obj "content" right)
        props (js-obj "element" "#acediff" "left" left "right" right)]
    (js/AceDiff. props)))

(defn highlight-code-block
  [editable? type]
  (js/setTimeout #(highlight-ace-code-block! editable? (get ace-theme-mapping type "json")) 75))

(defn compare-code-block
  [{:keys [left right]}]
  (js/setTimeout #(compare-ace-code-block! left right) 75))

(defn get-input-value
  [v]
  (-> v .-target .-value))

(defn on-filter-change
  [type value]
  (swap! state assoc-in [:filters type] (get-input-value value)))

(defn update-file-data
  [val file-name]
  (let [wrapper (-> js/document
                    (.getElementsByClassName "overflow-class")
                    (aget 0))
        text    (.createTextNode js/document val)
        div     (.createElement js/document "div")
         _      (.setAttribute
                      div
                      "id"
                      "jsEditor")]
      (aset wrapper "textContent" "")
      (.appendChild div text)
      (.appendChild wrapper div))
  (highlight-ace-code-block! false (get ace-theme-mapping (utils/get-extension file-name) "json"))
  (swap! state
         #(-> %
              (assoc-in [:new-entry :val] val)
              (assoc-in [:new-entry :file-name] file-name)
              (assoc-in [:new-entry :type] (utils/get-extension file-name)))))

(defn hide-element [id]
  (let [elem        (js/document.getElementById id)
        style       (.-style elem)
        display     (.-display style)
        new-display (if (= "none" display) "block" "none")]
    (set! (.-display style) new-display)))

(defn on-input-change
  [type value]
  (swap! state assoc-in [:new-entry type] (get-input-value value)))

(defn enable-body-scroll
  []
  (js/document.body.classList.remove "disable-body-scroll"))

(defn disable-body-scroll
  []
  (js/document.body.classList.add "disable-body-scroll"))

(defn upload-file
  [input]
  (if (not (= "" (-> input .-target .-value)))
    (let [^js/File file (-> input .-target .-files (aget 0))
          reader (js/FileReader.)
          file-name (-> file .-name)]
      (.readAsText reader file)
      (set! (.-onload reader)
            (fn [e]
              (let [val (-> e .-target .-result)]
                (update-file-data val file-name)))))))
