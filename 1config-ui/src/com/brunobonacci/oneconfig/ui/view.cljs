(ns com.brunobonacci.oneconfig.ui.view
  (:require [com.brunobonacci.oneconfig.ui.utils :as utils]
            [com.brunobonacci.oneconfig.ui.controller :as ctl]
            [reagent.core :as reagent :refer [atom]]
            [re-frisk.core :as rf]
            [ajax.core :refer [GET POST]]
            [goog.string :as gs]
            [clojure.string :as string]))



(defn create-service-name-row [num sz key]
  (if (zero? num) [:td {:row-span sz} key]))



(defn create-table-extended
  [items]
  (for [item items]
    (let [{:keys [key env version change-num content-type master-key user]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class "centered aligned"}
       [create-service-name-row (.indexOf items item) (count items) key]
       [:td {:data-label "Environment"} (utils/as-label (utils/colourize-label env) env)]
       [:td {:data-label "Version"} version]
       [:td {:data-label "Change num"} change-num]
       [:td {:data-label "Time"} (utils/parse-date change-num)]
       [:td {:data-label "Type"} (utils/as-label content-type)]
       [:td {:data-label "Value"}
        [:button {:class "ui icon button" :on-click #(ctl/get-config-item! item)}
         [:i {:class "eye icon"}]]]
       [:td {:data-label "Master Key" :class "master-key-width"}
        [:div {:class "tooltip"} (utils/get-kms-uuid master-key)
         [:span {:class "tooltiptext"} master-key]]]
       [:td {:data-label "User"}
        [:div {:class "tooltip"} (utils/get-aws-username user)
         [:span {:class "tooltiptext"} user]]]])))



(defn create-minified-table
  [items]
  (for [item items]
    (let [{:keys [key env version change-num content-type]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class "centered aligned"}
       [create-service-name-row (.indexOf items item) (count items) key]
       [:td {:data-label "Environment"} (utils/as-label (utils/colourize-label env) env)]
       [:td {:data-label "Version"} version]
       [:td {:data-label "Change num"} change-num]
       [:td {:data-label "Time"} (utils/parse-date change-num)]
       [:td {:data-label "Type"} (utils/as-label content-type)]
       [:td {:data-label "Value"}
        [:button {:class "ui icon button" :on-click #(ctl/get-config-item! item)}
         [:i {:class "eye icon"}]]]])))


;; TODO: fix param
(defn add-config-entry-form
  [_ new-entry]
  [:form {:class "ui form" :on-submit #(ctl/add-config-entry! %1)}
   [:div {:class "ui grid"}
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "row onecfg-filter-block"}
      [:input {:type        "text"
               :placeholder "Service Name"
               :name        "service"
               :value       (get new-entry :key)
               :on-change   #(ctl/on-input-change :key %)}]]

     [:div {:class "row onecfg-filter-block"}
      [:input {:type        "text"
               :placeholder "Environment"
               :name        "environment"
               :value       (get new-entry :env)
               :on-change   #(ctl/on-input-change :env %)}]]

     [:div {:class "row onecfg-filter-block"}
      [:input {:type        "text"
               :placeholder "Version"
               :name        "version"
               :value       (get new-entry :version)
               :on-change   #(ctl/on-input-change :version %)}]]

     [:div {:class "row onecfg-filter-block"}
      [:select {:class     "ui dropdown modal-selector "
                :value     (get new-entry :type)
                :on-change #(ctl/on-input-change :type %)}

       [:option {:value "json"} "json"]
       [:option {:value "edn"} "edn"]
       [:option {:value "properties"} "properties"]
       [:option {:value "txt"} "txt"]]]

     [:div {:class "ui horizontal divider"} "Upload a file"]]
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
                  ;; TODO: extract function
                  :on-change (fn [this]
                               (if (not (= "" (-> this .-target .-value)))
                                 (let [^js/File file (-> this .-target .-files (aget 0))
                                       reader (js/FileReader.)
                                       file-name (-> file .-name)]
                                   (.readAsText reader file)
                                   (set! (.-onload reader)
                                         (fn [e]
                                           (let [val (-> e .-target .-result)]
                                             (ctl/update-file-data val file-name)))))))}]

         [:label {:for "file-input" :class "ui mini blue button"}
          [:i {:class "ui upload icon"}] "Upload"]]
        [:td (get new-entry :file-name)]
        [:td
         (if (gs/isEmptyString (get new-entry :file-name))
           [:i]
           [:i {:class "red trash icon" :on-click #(ctl/remove-file!)}])]]]]]
    [:div {:class "two wide column"}]

    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "twelve wide column"}
     [:div {:class "ui horizontal divider"} "or provide config here"]
     [:div {:class "column"}
      [:textarea {:class       "modal-textarea"
                  :placeholder "Config data..."
                  :value       (get new-entry :val)
                  :on-change   #(ctl/on-input-change :val %)}]]]
    [:div {:class "two wide column"}]

    ;;;-------------------------------------------------
    [:div {:class "two wide column"}]
    [:div {:class "four wide column"}
     [:button {:class "ui primary button"} "Save"]]
    [:div {:class "four wide column"}]
    [:div {:class "four wide column "}
     [:button {:class "ui grey button right floated left aligned" :on-click #(ctl/close-new-entry-panel!)} "Close"]]
    [:div {:class "two wide column"}]]])



;; TODO: fix param
(defn table-filter-section
  [_ filters]
  [:tr {:class "center aligned"}
   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "key-input-width"
              :placeholder "Service Name.."
              :value       (get filters :key)
              :on-change   #(ctl/on-filter-change :key %)}]
     [:i {:class "search icon"}]]]

   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "env-input-width"
              :placeholder "Environment.."
              :value       (get filters :env)
              :on-change   #(ctl/on-filter-change :env %)}]
     [:i {:class "search icon"}]]]

   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "version-input-width"
              :placeholder "Version.."
              :value       (get filters :version)
              :on-change   #(ctl/on-filter-change :version %)}
      ]
     [:i {:class "search icon"}]]]])



;; TODO: fix param
(defn table-header-extended
  [_ filters]
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
    [:th {:row-span 2} "User"]]
   [table-filter-section :DUMMY filters]])



;; TODO: fix param
(defn table-header
  [_ filters]
  [:thead
   [:tr {:class "center aligned"}
    [:th "Service"]
    [:th "Environment"]
    [:th "Version"]
    [:th {:row-span 2} "Change num"]
    [:th {:row-span 2} "Time"]
    [:th {:row-span 2} "Type"]
    [:th {:row-span 2} "Value"]]
   [table-filter-section :DUMMY filters]])



(defn get-label-text
  [extended-mode?]
  [:div {:class "ui blue label"}
   (if extended-mode? "extended mode" "minified mode")])



(defn show-extended-table
  [items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header-extended :DUMMY filters]
   [:tbody
    (for [itm items]
      (create-table-extended (val itm)))]])



(defn show-minified-table
  [items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header :DUMMY filters]
   [:tbody
    (for [itm items]
      (create-minified-table (val itm)))]])



(defn create-config-table
  [extended-mode? real-filters items]
  [:div {:class "sixteen wide column config-table-scroll"}
   [:div {:class "ui grid"}
    (if (true? extended-mode?)
      (show-extended-table items real-filters)
      (show-minified-table items real-filters))]])



(defn modal-window [item-params item-data]
  [:div {:class "ui grid" :style {:padding "16px"}}
   [:div {:class "three wide column"}
    [:table {:class "ui celled striped table"}
     [:thead
      [:tr
       [:th {:class "center aligned collapsing" :col-span "2"}
        (get item-params :key)]]]

     [:tbody
      [:tr
       [:td {:class "center aligned collapsing"} "Environment"]
       [:td {:class "center aligned collapsing"}
        (let [env (get item-params :env)]
          (utils/as-label (utils/colourize-label env) env))]]

      [:tr
       [:td {:class "center aligned collapsing"} "Version"]
       [:td {:class "center aligned collapsing"}
        (get item-params :version)]]

      [:tr
       [:td {:class "center aligned collapsing"} "Change num"]
       [:td {:class "center aligned collapsing"}
        (get item-params :change-num)]]

      [:tr
       [:td {:class "center aligned collapsing"} "Time"]
       [:td {:class "center aligned collapsing"} (utils/parse-date (get item-params :change-num))]]

      [:tr
       [:td {:class "center aligned collapsing"} "Type"]
       [:td {:class "center aligned collapsing"} (utils/as-label (get item-params :content-type))]]]]]

   [:div {:class "ten wide column"}
    [:div {:class "ui raised segment"}
     [:a {:class "ui blue ribbon label" :on-click #(ctl/copy-to-clipboard! item-data)} "Copy to clipboard"]
     [:a {:class "ui grey right ribbon label" :on-click #(ctl/toggle-modal!)} "Close details"]
     [:div  {:class "overflow-class"}
      (utils/as-code item-data)]]]
   [:div {:class "three wide column"}]

   ;;-----------------------------------------
   [:div {:class "three wide column"}]
   [:div {:class "ten wide column"}]
   [:div {:class "three wide column"}]])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| M A I N   P A G E |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn main-page
  [current-state]
  [:div
   [:div {:class (if (= :new-entry-mode (get current-state :client-mode))
                   "sidenav visible"
                   "sidenav hidden")}
    [add-config-entry-form :DUMMY (get current-state :new-entry)]]
   [:div {:class "sticky-nav-bar"}
    [:div {:class "ui secondary menu"}
     [:div {:class "item"}
      [:div {:class "ui inverted button" :on-click #(ctl/toggle-new-entry-panel! (get current-state :client-mode))} "New Entry"]]
     [:div {:class "right menu"}
      [:div {:class "item"}
       [:div
        (get-label-text (get current-state :extended-mode?))]]

      [:div {:class "item"}
       [:div
        [:label {:class "switch"}
         [:input {:type "checkbox" :on-click #(ctl/toggle-table-mode!)}]
         [:span {:class "slider round"}]]]]

      [:div {:class "item"}
       [:button {:class "circular ui inverted icon button "}
        [:i {:class "icon user outline"}]]]]]]

   [:div {:class "ui grid"}
    [:div {:class "sixteen wide column"}
     [create-config-table (get current-state :extended-mode?)
      (get current-state :filters)
      (group-by :key
                (utils/filter-entries
                 (get current-state :filters)
                 (get current-state :entries)))]

     (if (true? (get current-state :show-modal-window?))
       [:div {:class "modal show-modal"}
        [modal-window (get current-state :item-params)  (get current-state :item-data)]]
       [:div {:class "modal"}])]]
   [:div {:class "footer" }
    (ctl/footer-element (get current-state :1config-version))]])


;; render app-root with the current state
(defn app-root
  []
  (main-page @ctl/state))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Initialize App
(comment
  (defn dev-setup []
    (when ^boolean js/goog.DEBUG
      (enable-console-print!)
      (rf/enable-frisk!)
      (rf/add-data :app-state ctl/state))))



(defn ^:export main []
  #_(dev-setup)
  (ctl/get-all-configs!)
  (ctl/get-version!)
  (reagent/render [app-root]
                  (. js/document (getElementById "app"))))
