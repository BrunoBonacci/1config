(ns com.brunobonacci.oneconfig.ui.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [re-frisk.core :as rf]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pp]
    [com.brunobonacci.oneconfig.ui.comm :as comm]
    [com.brunobonacci.oneconfig.ui.popup.style :as surface]
    [com.brunobonacci.oneconfig.ui.popup.surface-13 :as surface-13]
    [clojure.string :as string]))


; for `println` to work
(def debug?
  ^boolean js/goog.DEBUG)
(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars
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
            :new-version-flag? nil

            :new-entry nil

            ;; modal window (initial as plain, should be nested)
            :page-key    :surface-13
            :item-data   nil
            :item-params nil
            ;:modal {
            ;        :page-key    :surface-13
            ;        :item-data   nil
            ;        :item-params nil
            ;        }

                      }))

;TODO maybe all atoms which manage states should be turned into a single "state"-atom
(defonce app-state-data (atom ""))
(defonce app-state-data-copy (atom ""))
(defonce footer-data (atom ""))
(defonce check-box-toggle (atom "minified-mode"))
(defonce sidenav-display-toggle (atom "sidenav hidden"))
(defonce file-upload-name (atom ""))
(defonce file-upload-style (atom "hide-element"))

(defonce submit-data (atom {:key     ""
                            :env     ""
                            :version ""
                            :type    ""
                            :val     ""
                            :file     ""
                            }))

(defonce search-data (atom {:key     ""
                            :env     ""
                            :version ""
                            }))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ajax Handlers

(defn error-handler [{:keys [status status-text]}]
  (pp/pprint (str "failure occurred: " status " " status-text)))

(defn error-handler-alert [response]
  (js/alert response))

(defn error-handler-console [response]
  (print response))

(defn get-item-handler [response]
  (swap! state assoc-in [:item-data] (get response :value))
  (swap! state update :page-key
         (fn [pk]
           (if (= pk :surface-13-modal)
             :surface-13
             :surface-13-modal))))

(defn get-all-configs-handler [data]
  (reset! app-state-data data)
  (reset! app-state-data-copy data)
  (swap! state assoc-in [:entries] data)                    ;; TODO temp solution
  )

(defn get-footer-text-handler [{:keys [description license version]}]
  (reset! footer-data (str "1Config." description license ". Bruno Bonacci, 2019, v. " version)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ajax

(defn get-all-configs! []
  (GET "/configs"
       {
        :handler         get-all-configs-handler
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))

(defn get-config-item! [item]
  (let [{:keys [key env version change-num content-type]} item
        get-url (str "/configs/keys/" key "/envs/" env "/versions/" version )]
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

(defn apply-filter! [search-data]
  (reset!  app-state-data (comm/filter-entries {:key     (get @search-data :key)
                                                :env     (get @search-data :env)
                                                :version (get @search-data :version)
                                                } @app-state-data-copy)))

(defn add-config-entry! [event form-state]
  (.preventDefault event)
  (let [form-data (doto
                    (js/FormData.)
                    (.append "key" (get @form-state :key))
                    (.append "env" (get @form-state :env))
                    (.append "version" (get @form-state :version))
                    (.append "content-type" (get @form-state :type))
                    (.append "value" (get @form-state :val)))]
    (reset! form-state nil)
    (POST "/configs" {
                      :body            form-data
                      :response-format :json
                      :keywords?       true
                      :handler         #(get-all-configs!)
                      :error-handler   error-handler
                      })))

(defn get-footer-text []
  (GET "/footer"
       {:handler         get-footer-text-handler
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))

(defn create-service-name-row [num sz key]
  (if (zero? num) [:td {:row-span sz} key]))

(defn create-table-extended [items]
  (for [item items]
    (let [{:keys [key env version change-num content-type master-key user]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class "top aligned"}
       [create-service-name-row (.indexOf items item) (count items) key]
       [:td {:data-label "Environment"} (comm/as-label (comm/colourize-label env) env)]
       [:td {:data-label "Version"} version]
       [:td {:data-label "Change num"} change-num]
       [:td {:data-label "Time"} (comm/parse-date change-num)]
       [:td {:data-label "Type"} (comm/as-label content-type)]
       [:td {:data-label "Value"}
        [:button {:class "ui icon button" :on-click #(get-config-item! item)}
         [:i {:class "ellipsis horizontal icon"}]]
        ]
       [:td {:data-label "Master Key" :class "master-key-width"}
        [:div {:class "tooltip"} (comm/get-kms-uuid master-key)
         [:span {:class "tooltiptext"} master-key]]
        ]
       [:td {:data-label "User"}
        [:div {:class "tooltip"} (comm/get-aws-username user)
         [:span {:class "tooltiptext"} user]]
        ]
       ]
      )
    )
  )

(defn create-minified-table [items]
  (for [item items]
    (let [{:keys [key env version change-num content-type]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class "top aligned"}
       [create-service-name-row (.indexOf items item) (count items) key]
       [:td {:data-label "Environment"} (comm/as-label (comm/colourize-label env) env)]
       [:td {:data-label "Version"} version]
       [:td {:data-label "Change num"} change-num]
       [:td {:data-label "Time"} (comm/parse-date change-num)]
       [:td {:data-label "Type"} (comm/as-label content-type)]
       [:td {:data-label "Value"}
        [:button {:class "ui icon button" :on-click #(get-config-item! item)}
         [:i {:class "ellipsis horizontal icon"}]]
        ]
       ]
      )
    )
  )

(defn notify-file-upload! [name]
  (if (= name "")
    (reset! file-upload-style "hide-element")
    (reset! file-upload-style "show-element")))



(defn sidenav-display!  []
  (if (= @sidenav-display-toggle "sidenav visible")
    (reset! sidenav-display-toggle "sidenav hidden")
    (reset! sidenav-display-toggle "sidenav visible"))
  )

(defn debug-output!  []
  (println "click event"))

(defn add-config-entry-form []
  [:form {:class "ui form" :on-submit #(add-config-entry! %1 submit-data)}
   [:div {:class "ui grid"}
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Service Name"
               :name        "service"
               :value       (get @submit-data :key)
               :on-change   (fn [evt]
                              (swap! submit-data assoc-in [:key] (-> evt .-target .-value)))
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Environment"
               :name        "environment"
               :value       (get @submit-data :env)
               :on-change   (fn [evt]
                              (swap! submit-data assoc-in [:env] (-> evt .-target .-value)))
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Version"
               :name        "version"
               :value       (get @submit-data :version)
               :on-change   (fn [evt]
                              (swap! submit-data assoc-in [:version] (-> evt .-target .-value))
                              )
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:select {:class "ui dropdown modal-selector "
                :on-change (fn [evt]
                             (swap! submit-data assoc-in [:type] (-> evt .-target .-value)))
                :value (get @submit-data :type)
                }
       [:option {:value "" :selected "true" :disabled "disabled" :hidden "true"} "Type"]
       [:option {:value "edn"} "edn"]
       [:option {:value "properties"} "properties"]
       [:option {:value "json"} "json"]
       [:option {:value "txt"} "txt"]
       ]
      ]
     [:div {:class "ui horizontal divider"} "Upload a file"]
     ]
    [:div {:class "two wide column"}]
    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     ;;~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     [:table {:class "ui very basic  table"}
      [:tbody
       [:tr
        [:td
         [:input {:type      "file"
                  :name      "file"
                  :class     "inputfile"
                  :id        "file-input"
                  :on-change (fn [this]
                               (if (not (= "" (-> this .-target .-value)))
                                 (let [^js/File file (-> this .-target .-files (aget 0))
                                       reader (js/FileReader.)
                                       file-name (-> file .-name) ;;TODO replace with it and rename
                                       ]
                                   (.readAsText reader file)
                                   (swap! submit-data assoc-in [:type] (comm/get-extension file-name))
                                   (set! (.-onload reader)
                                         (fn [e]
                                           (let [xxx (-> e .-target .-result)]
                                             (swap! submit-data assoc-in [:val] xxx)
                                             )
                                           ))
                                   (reset! file-upload-name file-name)
                                   (notify-file-upload! file-name)
                                   )
                                 )
                               )
                  }
          ]
         [:label {:for "file-input" :class "ui mini blue button"}
          [:i {:class "ui upload icon"}]"Upload"]
         ]
        [:td @file-upload-name]
        [:td
         [:i {:class "red trash icon" :on-click #(debug-output!)}]
         ]]]]
     ]
    [:div {:class "two wide column"}]
    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "ui horizontal divider"} "or provide config here"]
     [:div {:class "column"}
      [:textarea {:class "modal-textarea"
                  :placeholder "Config data..."
                  :value (get @submit-data :val)
                  :on-change  (fn [evt]
                                (swap! submit-data assoc-in [:val] (-> evt .-target .-value)))
                  }]
      ]
     ]
    [:div {:class "two wide column"}]
    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
      [:button {:class "ui primary button"} "Save" ]
     ]
    [:div {:class "two wide column"}
     ]
    ]
   ]
  )

(defn table-header-extended []
  [:thead
   [:tr {:class "center aligned"}
    [:th "Service"]
    [:th "Environment"]
    [:th "Version"]
    [:th {:row-span 2} "Change num"]
    [:th {:row-span 2} "Time"]
    [:th {:row-span 2} "Type"]
    [:th {:row-span 2} "Value"]
    [:th {:row-span 2} "Master Key"]
    [:th {:row-span 2} "User"]
    ]
   [:tr {:class "center aligned"}
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "key-input-width"
               :placeholder "Service Name.."
               :value       (get @search-data :key)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:key] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "env-input-width"
               :placeholder "Environment.."
               :value       (get @search-data :env)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:env] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "version-input-width"
               :placeholder "Version.."
               :value       (get @search-data :version)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:version] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    ]
   ]
  )

(defn table-header []
  [:thead
   [:tr {:class "center aligned"}
    [:th "Service"]
    [:th "Environment"]
    [:th "Version"]
    [:th {:row-span 2} "Change num"]
    [:th {:row-span 2} "Time"]
    [:th {:row-span 2} "Type"]
    [:th {:row-span 2} "Value"]
    ]
   [:tr {:class "center aligned"}
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "key-input-width"
               :placeholder "Service Name.."
               :value       (get @search-data :key)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:key] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "env-input-width"
               :placeholder "Environment.."
               :value       (get @search-data :env)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:env] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "version-input-width"
               :placeholder "Version.."
               :value       (get @search-data :version)
               :on-change   (fn [evt]
                              (swap! search-data assoc-in [:version] (-> evt .-target .-value))
                              (apply-filter! search-data))
               }]
      [:i {:class "search icon"}]]
     ]
    ]
   ]
  )

(defn show-table-mode!  []
  (if (= @check-box-toggle "extended-mode")
    (reset! check-box-toggle "minified-mode")
    (reset! check-box-toggle "extended-mode"))
  )

(defn get-label-text []
  (if (= @check-box-toggle "extended-mode")
    [:div {:class "ui blue label"}
     "minified mode"
      [:span {:class "minified-mode-label"}
       [:i {:class "inverted arrow alternate circle left outline icon"} ]
       ]
     ]
    [:div {:class "ui blue label"}
     "extended mode"
     [:span {:class "minified-mode-label"}
      [:i {:class "inverted arrow alternate circle right outline icon"} ]
      ]
     ]
    ))

(defn show-extended-table [items]
  [:table {:class "ui selectable celled table"}
   [table-header-extended]
   [:tbody
    (for [itm items]
      (create-table-extended (val itm))
      )
    ]
   ]
  )

(defn show-minified-table [items]
  [:table {:class "ui selectable celled table"}
   [table-header]
   [:tbody
    (for [itm items]
      (create-minified-table (val itm)))]
   ]
  )

(defn create-config-table [items]
  [:div {:class "sixteen wide column"}
   [:div {:class "ui grid"}
    (if (= @check-box-toggle "extended-mode")
      (show-extended-table items)
      (show-minified-table items))
    ]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;https://stackoverflow.com/questions/29581359/semantic-ui-ui-grid-best-approach-for-layout-rows-columns-vs-segments
;Semantic UI - ui grid best approach for layout (rows/columns vs segments)
(defn app-root [appRootDataState]
  [:div
   [:div {:class @sidenav-display-toggle}
    [add-config-entry-form]
    ]
   [:div {:class "sticky-nav-bar"}
    [:div {:class "ui secondary menu"}
     [:div {:class "item"}
      [:div {:class "ui inverted button" :on-click #(sidenav-display!)} "New Entry"]
      ]
     [:div {:class "right menu"}
      [:div {:class "item"}
       [:div
        (get-label-text)
        ]
       ]
      [:div {:class "item"}
       [:div
        [:label {:class "switch"}
         [:input {:type "checkbox" :on-click #(show-table-mode!)  }   ]
         [:span {:class "slider round"}]]
        ]
       ]

      [:div {:class "item"}
       [:button {:class "circular ui inverted icon button "}
        [:i {:class "icon user outline"}]]]
      ]
     ]
    ]
   [:div {:class "ui grid"}
    [:div {:class "sixteen wide column"}
     [surface/surface {:app-state          appRootDataState
                       :surface-key        (get @appRootDataState :page-key)
                       :surface-registry   surface-13/surfaces
                       :component-registry surface-13/components
                       }]
     [create-config-table (group-by :key (get @appRootDataState :entries))]
     ]
    ]
   [:div {:class "footer" } @footer-data]
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App
(defn dev-setup []
  (when ^boolean js/goog.DEBUG
    (enable-console-print!)
    (rf/enable-frisk!)
    (rf/add-data :app-state state)
    ))

(defn reload [reloadDataState]
  (reagent/render [app-root reloadDataState]
                  (. js/document (getElementById "app"))
                  )
  )

(defn ^:export main []
  (dev-setup)
  ;(app-routes state)
  (get-all-configs!)
  (get-footer-text)
  (reload state))