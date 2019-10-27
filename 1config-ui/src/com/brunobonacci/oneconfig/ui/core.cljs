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
            ;:new-version-flag? nil
            :1config-version {:current "" :latest ""}

            :new-entry nil

            ;; modal window (initial as plain, should be nested) it should be  ":show-entry-mode"
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
        :handler         #(swap! state assoc-in [:entries] %)
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

(defn apply-filters [filters entries]
  (comm/filter-entries {:key     (get filters :key)
                        :env     (get filters :env)
                        :version (get filters :version)
                        } entries))

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

(defn get-footer-text [allState]
  (GET "/footer"
       {:handler          #(swap! allState assoc-in [:1config-version] %)
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
       [:option {:value "json" :selected "true" } "json"]
       [:option {:value "edn"} "edn"]
       [:option {:value "properties"} "properties"]
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
               :value       (get-in @state [:filters :key])
               :on-change   #(swap! state assoc-in [:filters :key] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "env-input-width"
               :placeholder "Environment.."
               :value       (get-in @state [:filters :env])
               :on-change   #(swap! state assoc-in [:filters :env] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "version-input-width"
               :placeholder "Version.."
               :value       (get-in @state [:filters :version])
               :on-change   #(swap! state assoc-in [:filters :version] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    ]
   ]
  )
;; TODO we need to pass state as a parameter here
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
               :value       (get-in @state [:filters :key])
               :on-change   #(swap! state assoc-in [:filters :key] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "env-input-width"
               :placeholder "Environment.."
               :value       (get-in @state [:filters :env])
               :on-change   #(swap! state assoc-in [:filters :env] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    [:th
     [:div {:class "ui mini icon input"}
      [:input {:type        "text"
               :class       "version-input-width"
               :placeholder "Version.."
               :value       (get-in @state [:filters :version])
               :on-change   #(swap! state assoc-in [:filters :version] (-> % .-target .-value))
               }]
      [:i {:class "search icon"}]]
     ]
    ]
   ]
  )

(defn get-label-text [extended-mode?]
  (if (true? extended-mode?)
    [:div {:class "ui blue label"}
     "back to extended mode"
     [:span {:class "minified-mode-label"}
      [:i {:class "inverted arrow alternate circle right outline icon"} ]
      ]
     ]
    [:div {:class "ui blue label"}
     "back to minified mode"
     [:span {:class "minified-mode-label"}
      [:i {:class "inverted arrow alternate circle left outline icon"} ]
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

(defn create-config-table [extended-mode? items]
  [:div {:class "sixteen wide column"}
   [:div {:class "ui grid"}
    (if (true? extended-mode?)
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
        (get-label-text (get @appRootDataState :extended-mode?))
        ]
       ]
      [:div {:class "item"}
       [:div
        [:label {:class "switch"}
         [:input {:type "checkbox" :on-click #(swap! appRootDataState update-in [:extended-mode?] not) }   ]
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
     [create-config-table (get @appRootDataState :extended-mode?) (group-by :key
                                    (apply-filters
                                      (get @appRootDataState :filters)
                                      (get @appRootDataState :entries)
                                      )
                                    )]
     ]
    ]
   [footer-element (get @appRootDataState :1config-version)]
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
  (get-footer-text state)
  (reload state))