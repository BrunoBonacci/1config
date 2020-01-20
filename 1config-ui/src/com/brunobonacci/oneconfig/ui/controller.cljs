(ns com.brunobonacci.oneconfig.ui.controller
  (:require
   [reagent.core :as reagent :refer [atom]]
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

    ;; modal window (initial as plain, should be nested) it should be  ":show-entry-mode"
    :item-data   nil
    :item-params nil
    }))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                      ----==| H A N D L E R S |==----                       ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn error-handler [{:keys [status status-text]}]
  (pp/pprint (str "failure occurred: " status " " status-text)))



(defn update-version! [version]
  (swap! state assoc :1config-version version))



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
         (str "(Latest version v." (get version :latest) ")"))))


(defn get-preferences! []
  (GET "/preferences"
       {
        :handler         (fn [prefs]
                           (swap! state assoc :preferences prefs))
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))



(defn all-configs-handler! [entries]
  (swap! state
         (fn [s]
           (-> s
               (assoc :entries entries)
               (assoc :client-mode :listing)))))


(defn get-all-configs! []
  (GET "/configs"
       {
        :handler         all-configs-handler!
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))



(defn get-config-item! [item]
  (let [{:keys [key env version change-num content-type]} item
        get-url (gs/format "/configs/keys/%s/envs/%s/versions/%s"
                           (gs/urlEncode key) (gs/urlEncode env) (gs/urlEncode version))]

    (swap! state assoc-in [:item-params] {:key          key
                                          :env          env
                                          :version      version
                                          :change-num   change-num
                                          :content-type content-type})

    (GET get-url {:handler         get-item-handler
                  :format          :json
                  :response-format :json
                  :keywords?       true
                  :error-handler   error-handler})))



(defn add-config-entry! [event]
  (.preventDefault event)
  (let [new-entry (get @state :new-entry)
        form-data (doto
                      (js/FormData.)
                    (.append "key"          (get new-entry :key))
                    (.append "env"          (get new-entry :env))
                    (.append "version"      (get new-entry :version))
                    (.append "content-type" (get new-entry :type))
                    (.append "value"        (get new-entry :val)))]

    (swap! state assoc-in [:new-entry] empty-new-entry)

    (POST "/configs" {:body            form-data
                      :response-format :json
                      :keywords?       true
                      :handler         get-all-configs!
                      :error-handler   error-handler})))



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


;;TODO rename
(defn toggle-new-entry-panel! [mode]
  (cond
    (= :listing mode)         (swap! state assoc-in [:client-mode] :new-entry-mode)
    (= :new-entry-mode mode)  (swap! state assoc-in [:client-mode] :edit-entry-mode)
    (= :show-entry-mode mode) (swap! state #(-> %
                                              (assoc-in [:new-entry] empty-new-entry)
                                              (assoc-in [:client-mode] :edit-entry-mode)))
    (= :edit-entry-mode mode) (swap! state #(-> %
                                              (assoc-in [:new-entry] empty-new-entry)
                                              (assoc-in [:client-mode] :new-entry-mode)))
    :else (println (str "unknown mode : " mode)))
  )

;;TODO rename
(defn map-to-object
  [item-params item-data]
  (swap! state assoc-in [:new-entry] {:key       (get item-params :key)
                                      :env       (get item-params :env)
                                      :version   (get item-params :version)
                                      :type      (get item-params :content-type)
                                      :val       item-data
                                      :file      ""
                                      :file-name ""
                                      }))

(defn toggle-table-mode!  []
  (swap! state update-in [:extended-mode?] not))



(defn toggle-modal!  []
  (swap! state assoc-in [:client-mode] :listing))



;;https://github.com/search?l=Clojure&p=2&q=.execCommand+js%2Fdocument&type=Code
(defn copy-to-clipboard! [t]
  (let [e (. js/document (createElement "textarea"))]
    (.. js/document -body (appendChild e))
    (set! (.-value e) t)
    (.select e)
    (.setSelectionRange e 0 99999)
    (. js/document (execCommand "copy"))
    (.. js/document -body (removeChild e)))
  (js/alert "Copied to clipboard"))

(defn highlight-code-block []
  (js/setTimeout #(->> (js/document.querySelector "code")
                       (js/hljs.highlightBlock)) 90))

(defn close-new-entry-panel!  []
  (swap! state assoc-in [:client-mode] :listing))



(defn get-input-value
  [v]
  (-> v .-target .-value))



(defn on-filter-change
  [type value]
  (swap! state assoc-in [:filters type] (get-input-value value)))



(defn update-file-data
  [val file-name]
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

(defn upload-file [input]
  (if (not (= "" (-> input .-target .-value)))
    (let [^js/File file (-> input .-target .-files (aget 0))
          reader (js/FileReader.)
          file-name (-> file .-name)]
      (.readAsText reader file)
      (set! (.-onload reader)
            (fn [e]
              (let [val (-> e .-target .-result)]
                (update-file-data val file-name)))))))
