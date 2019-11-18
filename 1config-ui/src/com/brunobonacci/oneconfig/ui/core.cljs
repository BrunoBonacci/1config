(ns com.brunobonacci.oneconfig.ui.core
  (:require
    [reagent.core :as reagent :refer [atom]]
    [re-frisk.core :as rf]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pp]
    [com.brunobonacci.oneconfig.ui.comm :as comm]
    [goog.string :as gs]
    [clojure.string :as string]))


; for `println` to work
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
  (comm/filter-entries {:key     (get filters :key)
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

(defn get-footer-text []
  (GET "/footer"
       {:handler          update-version!
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
      [:tr {:class "centered aligned"}
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
      [:tr {:class "centered aligned"}
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
                  (assoc-in [:new-entry :type] (comm/get-extension file-name))
                 )))

(defn on-input-change
  [type value]
 (swap! state assoc-in [:new-entry type] (get-input-value value)))

(defn add-config-entry-form [dummy new-entry]
  [:form {:class "ui form" :on-submit #(add-config-entry! %1)}
   [:div {:class "ui grid"}
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Service Name"
               :name        "service"
               :value       (get new-entry :key)
               :on-change   #(on-input-change :key %)
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Environment"
               :name        "environment"
               :value       (get new-entry :env)
               :on-change   #(on-input-change :env %)
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:input {
               :type        "text"
               :placeholder "Version"
               :name        "version"
               :value       (get new-entry :version)
               :on-change   #(on-input-change :version %)
               }]]
     [:div {:class "row onecfg-filter-block"}
      [:select {:class     "ui dropdown modal-selector "
                :value     (get new-entry :type)
                :on-change #(on-input-change :type %)
                }
       [:option {:value "json"} "json"]
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
                                       file-name (-> file .-name)]
                                   (.readAsText reader file)
                                   (set! (.-onload reader)
                                         (fn [e]
                                           (let [val (-> e .-target .-result)]
                                             (update-file-data val file-name)
                                             )
                                           ))
                                   )
                                 )
                               )
                  }
          ]
         [:label {:for "file-input" :class "ui mini blue button"}
          [:i {:class "ui upload icon"}] "Upload"]
         ]
        [:td (get new-entry :file-name)]
        [:td
         (if (gs/isEmptyString (get new-entry :file-name))
           [:i]
           [:i {:class "red trash icon" :on-click #(remove-file!)}])
         ]]]]
     ]
    [:div {:class "two wide column"}]
    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "ui horizontal divider"} "or provide config here"]
     [:div {:class "column"}
      [:textarea {:class       "modal-textarea"
                  :placeholder "Config data..."
                  :value       (get new-entry :val)
                  :on-change   #(on-input-change :val %)
                  }]
      ]
     ]
    [:div {:class "two wide column"}]
    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "four wide column"}
     [:button {:class "ui primary button"} "Save"]
     ]
    [:div {:class "four wide column"}]
    [:div {:class "four wide column "}
     [:button {:class "ui grey button right floated left aligned" :on-click #(close-new-entry-panel!)} "Close"]
     ]
    [:div {:class "two wide column"}
     ]
    ]
   ]
  )

(defn table-filter-section
  [dummy filters]
  [:tr {:class "center aligned"}
   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "key-input-width"
              :placeholder "Service Name.."
              :value       (get filters :key)
              :on-change   #(on-filter-change :key %)
              }]
     [:i {:class "search icon"}]]
    ]
   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "env-input-width"
              :placeholder "Environment.."
              :value       (get filters :env)
              :on-change   #(on-filter-change :env %)
              }]
     [:i {:class "search icon"}]]
    ]
   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "version-input-width"
              :placeholder "Version.."
              :value       (get filters :version)
              :on-change   #(on-filter-change :version %)
              }]
     [:i {:class "search icon"}]]
    ]
   ]
  )

(defn table-header-extended [tableExtendedFiltersData filters]
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
   [table-filter-section tableExtendedFiltersData filters]
   ]
  )

(defn table-header [filtersData filters]
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
   [table-filter-section filtersData filters]
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

(defn show-extended-table [items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header-extended "DUMMY" filters]
   [:tbody
    (for [itm items]
      (create-table-extended (val itm))
      )
    ]
   ]
  )

(defn show-minified-table [items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header "DUMMY" filters]
   [:tbody
    (for [itm items]
      (create-minified-table (val itm)))]
   ]
  )

(defn create-config-table [extended-mode? real-filters items]
  [:div {:class "sixteen wide column config-table-scroll"}
   [:div {:class "ui grid"}
    (if (true? extended-mode?)
      (show-extended-table items real-filters)
      (show-minified-table items real-filters))
    ]])

(defn modal-window [item-params item-data]
  [:div {:class "ui grid" :style {:padding "16px"}}
   [:div {:class "three wide column"}
     [:table {:class "ui celled striped table"}
      [:thead
       [:tr
        [:th {:class "center aligned collapsing" :col-span "2"}
         (get item-params :key)
         ]]]
      [:tbody
       [:tr
        [:td {:class "center aligned collapsing"} "Environment"]
        [:td {:class "center aligned collapsing"}
         (let [env (get item-params :env)]
           (comm/as-label (comm/colourize-label env) env))
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Version"]
        [:td {:class "center aligned collapsing"}
         (get item-params :version)
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Change num"]
        [:td {:class "center aligned collapsing"}
         (get item-params :change-num)
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Time"]
        [:td {:class "center aligned collapsing"} (comm/parse-date (get item-params :change-num))
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Type"]
        [:td {:class "center aligned collapsing"} (comm/as-label (get item-params :content-type))
         ]
        ]
       ]]
    ]
   [:div {:class "ten wide column"}
    [:div {:class "ui raised segment"}
     [:a {:class "ui blue ribbon label"} "Value"]
     [:div  {:class "overflow-class"}
      (comm/as-code item-data)
      ]
     ]
    ]
   [:div {:class "three wide column"}
    [:div {:class "modal-content"}
     [:span {:class "close-button" :on-click #(toggle-modal!)} "X"]
     [:h1 "close modal"]
     ]
    ]
   ;;-----------------------------------------
   [:div {:class "three wide column"}]
   [:div {:class "ten wide column"}]
   [:div {:class "three wide column"}]
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;https://stackoverflow.com/questions/29581359/semantic-ui-ui-grid-best-approach-for-layout-rows-columns-vs-segments
;Semantic UI - ui grid best approach for layout (rows/columns vs segments)
(defn app-root [state-atom]
  (let [current-state @state-atom]
    [:div
        [:div {:class (if (= :new-entry-mode (get current-state :client-mode))
                        "sidenav visible"
                        "sidenav hidden")}
         [add-config-entry-form "dummy" (get current-state :new-entry)]
         ]
        [:div {:class "sticky-nav-bar"}
         [:div {:class "ui secondary menu"}
          [:div {:class "item"}
           [:div {:class "ui inverted button" :on-click #(toggle-new-entry-panel! (get current-state :client-mode))} "New Entry"]
           ]
          [:div {:class "right menu"}
           [:div {:class "item"}
            [:div
             (get-label-text (get current-state :extended-mode?))
             ]
            ]
           [:div {:class "item"}
            [:div
             [:label {:class "switch"}
              [:input {:type "checkbox" :on-click #(toggle-table-mode!)}]
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
         [create-config-table (get current-state :extended-mode?)
          (get current-state :filters)
          (group-by :key
                    (apply-filters
                      (get current-state :filters)
                      (get current-state :entries)
                      ))]
         (if (true? (get current-state :show-modal-window?))
           [:div {:class "modal show-modal"}
            [modal-window (get current-state :item-params)  (get current-state :item-data)]
            ]
           [:div {:class "modal"}]
           )
         ]
        ]
     [footer-element (get current-state :1config-version)]
     ]
    );------------------------------
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App
(defn dev-setup []
  (when ^boolean js/goog.DEBUG
    (enable-console-print!)
    (rf/enable-frisk!)
    (rf/add-data :app-state state)
    ))

(defn ^:export main []
  (dev-setup)
  (get-all-configs!)
  (get-footer-text)
  (reagent/render [app-root state]
                  (. js/document (getElementById "app"))))