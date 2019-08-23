(ns com.brunobonacci.oneconfig.ui.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require
    [reagent.core :as reagent :refer [atom]]
    [re-frisk.core :as rf]
    [ajax.core :refer [GET POST]]
    [cljs.pprint :as pp]
    [com.brunobonacci.oneconfig.ui.comm :as comm]
    [clojure.string :as string]
    [com.brunobonacci.oneconfig.ui.popup.style :as surface]
    [com.brunobonacci.oneconfig.ui.popup.surface-13 :as surface-13]
    [cljs.core.async :as a :refer [<!]]))


; for `println` to work
(def debug?
  ^boolean js/goog.DEBUG)
(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Vars
(defonce app-state (atom {:page-key    :surface-13
                          :item-data   nil
                          :item-params nil}))
;TODO maybe all atoms which manage states should be turned into a single "state"-atom
(defonce app-state-data (atom ""))
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
  (swap! app-state assoc-in [:item-data] response)
  (swap! app-state update :page-key
         (fn [pk]
           (if (= pk :surface-13-modal)
             :surface-13
             :surface-13-modal))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Ajax

(defn load-all-configs! []
  (GET "/oneconfig-list"
       {
        :handler         #(reset! app-state-data %)
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))

(defn get-config-item! [item]
  (let [{:keys [key env version change-num content-type]} item]
    (swap! app-state assoc-in [:item-params] {:key          key
                                              :env          env
                                              :version      version
                                              :change-num   change-num
                                              :content-type content-type})
    (GET "/oneconfig-get-item"
         {:params          {:key          key
                            :env          env
                            :version      version
                            :change-num   change-num
                            :content-type content-type}
          :handler         get-item-handler
          :format          :json
          :response-format :json
          :keywords?       true
          :error-handler   error-handler})))

(defn apply-filter! [env]
  (GET "/oneconfig-apply-filter"
       {:params          {:key     (get @env :key)
                          :env     (get @env :env)
                          :version (get @env :version)
                          }
        :handler         #(reset! app-state-data %)
        :format          :json
        :response-format :json
        :keywords?       true
        :error-handler   error-handler}))

(defn add-config-entry! [event form-state]
  (.preventDefault event)
  (if-let [files (-> (.getElementById js/document "file-input") .-files)]
    (let [form-data (doto
                      (js/FormData.)
                      (.append "current-files" (aget files 0))
                      (.append "key"  (get @form-state :key))
                      (.append "env"  (get @form-state :env))
                      (.append "version"  (get @form-state :version))
                      (.append "content-type"  (get @form-state :type))
                      (.append "value"  (get @form-state :val)))]
      (reset! form-state nil)
      (POST "/oneconfig-add-entry" {
                                    :body            form-data
                                    :response-format :json
                                    :keywords?       true
                                    :handler         #(reset! app-state-data %)
                                    :error-handler   error-handler
                                    })
      )
    )
  )

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
    (reset! file-upload-style "ui compact menu hide-element")
    (reset! file-upload-style "ui compact menu show-element")))



(defn sidenav-display!  []
  (if (= @sidenav-display-toggle "sidenav visible")
    (reset! sidenav-display-toggle "sidenav hidden")
    (reset! sidenav-display-toggle "sidenav visible"))
  )

(defn add-config-entry-form-adv []
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
               ;:class @check-box-toggle
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
       [:option {:value "yml"} "yml"]
       [:option {:value "json"} "json"]
       [:option {:value "txt"} "txt"]
       ]
      ]
     ]
    [:div {:class "two wide column"}]
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "column"}

      [:label {:for "file-input"}
       [:a {:class "ui label" :for "file-input"}
        [:i {:class "cloud upload icon"}] "Upload a config file" ]
       ]
      [:input {
               :id    "file-input"
               :class "hide-element"
               :type  "file"
               :name  "file"
               :on-change
                      (fn [this]
                        (if (not (= "" (-> this .-target .-value)))
                          (let [^js/File file (-> this .-target .-files (aget 0))
                                reader (js/FileReader.)
                                file-name (-> file  .-name) ;;TODO replace with it and rename
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
               }]
      [:div {:class @file-upload-style}
       [:a {:class "item"}
        [:i {:class "icon file alternate outline"}] @file-upload-name
        [:div {:class "floating ui red label"} "1"]]
       ]
      ]
     [:div {:class "ui horizontal divider"} "or"]
     [:div {:class "column"}
      [:textarea {:class "modal-textarea"
                  :placeholder "Provide config here"
                  :value (get @submit-data :val)
                  :on-change  (fn [evt]
                                (swap! submit-data assoc-in [:val] (-> evt .-target .-value)))
                  }]
      ]
     ]
    [:div {:class "two wide column"}]
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:button {:class "ui primary button"}
      [:i {:class "plus icon"}] "Save Config Entry"]
     ]
    [:div {:class "two wide column"}]
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
    "switch to minified mode"
    "switch to extended mode"))

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

(defn create-table-wrapper [items]
  [:div {:class "sixteen wide column"}
   [:div {:class "ui grid"}
    (if (= @check-box-toggle "extended-mode")
      (show-extended-table items)
      (show-minified-table items))
    ]])

(defn list-oneconfig-data []
  (load-all-configs!)
  #(create-table-wrapper (group-by :key @app-state-data)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page
;https://stackoverflow.com/questions/29581359/semantic-ui-ui-grid-best-approach-for-layout-rows-columns-vs-segments
;Semantic UI - ui grid best approach for layout (rows/columns vs segments)
(defn app-root [app-state]
  [:div
   [:div {:class @sidenav-display-toggle}
    [add-config-entry-form-adv]
    ]
   [:h2
    [:div {:class "ui dividing header"}
     [:i {:class "file code outline icon"}]
     [:div {:class "content"} "1Config"
      [:div {:class "sub header"} "manage multiple environments and application configuration safely and effectively"]]]
    ]
   [:div {:class "ui grid"}
    [:div {:class "three wide column"}
     [:div {:class "two wide column"}]
     ]
    [:div {:class "ten wide column"}]
    [:div {:class "three wide column"}
     [:h2 {:class "ui icon header"}
      [:i {:class "circular plus square icon" :on-click #(sidenav-display!)}]
      [:div {:class "content"} "Add Entry Menu"]]
     [:div {:class "ui toggle checkbox"}
      [:input {:type "checkbox" :name "public" :on-click #(show-table-mode!)}]
      [:label (get-label-text)]]
     ]
    [:div {:class "sixteen wide column"}
     [surface/surface {:app-state          app-state
                       :surface-key        (get @app-state :page-key)
                       :surface-registry   surface-13/surfaces
                       :component-registry surface-13/components
                       }]
     [list-oneconfig-data]
     ]
    ]
   ]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App
(defn dev-setup []
  (when ^boolean js/goog.DEBUG
    (enable-console-print!)
    (rf/enable-frisk!)
    (rf/add-data :app-state app-state)
    ))

(defn reload []
  (reagent/render [app-root app-state]
                  (. js/document (getElementById "app"))
                  )
  )

(defn ^:export main []
  (dev-setup)
  ;(app-routes app-state)
  (reload))