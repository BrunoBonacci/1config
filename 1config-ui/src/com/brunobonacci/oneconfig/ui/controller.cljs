(ns com.brunobonacci.oneconfig.ui.controller
  (:require
    [reagent.core :as reagent :refer [atom]]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pp]
    [com.brunobonacci.oneconfig.ui.utils :as utils]
    [goog.string :as gs]
    ))

(def debug?
  ^boolean js/goog.DEBUG)
(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars
(def empty-new-entry {:key     ""
                      :env     ""
                      :version ""
                      :type    ""
                      :val     ""
                      :file    ""
                      :file-name ""
                      })
(defonce state
         (atom
           {
            ;; contains the config entries retrieved from the server
            :entries []
            ;; filters
            :filters {:key "", :env "", :version ""}
            ;; manages the toggle for the extended mode
            :extended-mode? false
            ;; one of :listing, :new-entry-mode, :show-entry-mode
            :client-mode :listing
            ;:new-version-flag? nil
            :1config-version {:current "" :latest ""}

            :new-entry empty-new-entry

            ;; modal window (initial as plain, should be nested) it should be  ":show-entry-mode"
            :page-key    :surface-13
            :item-data   nil
            :item-params nil
            ;:modal {
            ;        :page-key    :surface-13
            ;        :item-data   nil
            ;        :item-params nil
            ;        }

            ;; temp modal window
            :show-modal-window? false
            }))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ajax Handlers

(defn error-handler [{:keys [status status-text]}]
  (pp/pprint (str "failure occurred: " status " " status-text)))

(defn error-handler-alert [response]
  (js/alert response))

(defn error-handler-console [response]
  (print response))

(defn all-configs-handler! [entries]
  (swap! state assoc-in [:entries] entries)
  (swap! state assoc-in [:client-mode] :listing))

(defn update-version! [version]
  (swap! state assoc-in [:1config-version] version))

(defn get-item-handler [response]
  (swap! state assoc-in [:item-data] (get response :encoded-value))
  (swap! state update-in [:show-modal-window?] not)
  )

(defn footer-element [version]
  [:div {:class "footer" }
   (str "1Config.A library to manage multiple environments and application configuration safely and effectively.Apache License 2.0. Bruno Bonacci, 2019, v." (get version :current)
        (if (= (get version :current) (get version :latest))
          ""
          (str "(Latest version v." (get version :latest) ")")))
   ])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ajax

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
                           (gs/urlEncode key) (gs/urlEncode env) (gs/urlEncode version))
        ]
    (swap! state assoc-in [:item-params] {:key          key
                                          :env          env
                                          :version      version
                                          :change-num   change-num
                                          :content-type content-type})
    (GET get-url {
                  :handler         get-item-handler
                  :format          :json
                  :response-format :json
                  :keywords?       true
                  :error-handler   error-handler})))

(defn apply-filters [filters entries]
  (utils/filter-entries {:key     (get filters :key)
                        :env     (get filters :env)
                        :version (get filters :version)
                        } entries))

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
    (POST "/configs" {
                      :body            form-data
                      :response-format :json
                      :keywords?       true
                      :handler         get-all-configs!
                      :error-handler   error-handler
                      })))

(defn get-version! []
  (GET "/info/versions"
       {:handler          update-version!
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State Handlers

(defn remove-file! []
  (swap! state
         #(-> %
              (assoc-in [:new-entry :val] "")
              (assoc-in [:new-entry :file-name] ""))))

(defn toggle-new-entry-panel!  [mode]
  (if (= :new-entry-mode mode)
    (swap! state assoc-in [:client-mode] :listing)
    (swap! state assoc-in [:client-mode] :new-entry-mode)))

(defn toggle-table-mode!  []
  (swap! state update-in [:extended-mode?] not))

(defn toggle-modal!  []
  (swap! state update-in [:show-modal-window?] not))

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
  (swap! state #(-> %
                    (assoc-in [:new-entry :val] val)
                    (assoc-in [:new-entry :file-name] file-name)
                    (assoc-in [:new-entry :type] (utils/get-extension file-name))
                    )))

(defn on-input-change
  [type value]
  (swap! state assoc-in [:new-entry type] (get-input-value value)))