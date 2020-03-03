(ns com.brunobonacci.oneconfig.ui.view
  (:require [com.brunobonacci.oneconfig.ui.utils :as utils]
            [com.brunobonacci.oneconfig.ui.controller :as ctl]
            [reagent.core :as reagent :refer [atom]]
            [re-frisk.core :as rf]
            [goog.string :as gs]
            [clojure.string :as string]))



(defn colourize-label
  "find a color for a label by environment"
  [preferences env]
  ;; TODO: fix transport should be transit format
  (or (get-in preferences [:colors :env-labels (keyword env)])
      (get-in preferences [:colors :env-labels :default] "grey")))




(defn create-table-extended
  [preferences items]
  (for [item items]
    (let [{:keys [key env version change-num content-type master-key user]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class "center aligned"}
       [:td {:data-label "Key"} key]
       [:td {:data-label "Environment"} (utils/as-label (colourize-label preferences env) env)]
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
  [preferences items]
  (for [item items]
    (let [{:keys [key env version change-num content-type]} item]
      ^{:key (string/join "-" [key env version change-num content-type])}
      [:tr {:class (str "center aligned " (string/join "-" [key env version change-num content-type]))}
       [:td {:data-label "Key"} key]
       [:td {:data-label "Environment"} (utils/as-label (colourize-label preferences env) env)]
       [:td {:data-label "Version"} version]
       [:td {:data-label "Change num"} change-num]
       [:td {:data-label "Time"} (utils/parse-date change-num)]
       [:td {:data-label "Type"} (utils/as-label content-type)]
       [:td {:data-label "Value"}
        [:button {:class "ui icon button" :on-click #(ctl/get-config-item! item)}
         [:i {:class "eye icon"}]]]
       [:td {:data-label "Compare"}
        [:input {:type "checkbox" :on-change #(ctl/row-selected % item)}]]])))


;; TODO: fix param
(defn table-filter-section
  [_ filters]
  [:tr {:class "center aligned"}
   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "filter-input-width"
              :placeholder "Service Name.."
              :value       (get filters :key)
              :on-change   #(ctl/on-filter-change :key %)}]
     [:i {:class "search icon"}]]]

   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "filter-input-width"
              :placeholder "Environment.."
              :value       (get filters :env)
              :on-change   #(ctl/on-filter-change :env %)}]
     [:i {:class "search icon"}]]]

   [:th
    [:div {:class "ui mini icon input"}
     [:input {:type        "text"
              :class       "filter-input-width"
              :placeholder "Version.."
              :value       (get filters :version)
              :on-change   #(ctl/on-filter-change :version %)}]
     [:i {:class "search icon"}]]]
   [:th
    [:input {:type "checkbox" :on-change #(ctl/all-rows-selected %)}]]])



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
    [:th {:class "shrink-column"} "Environment"]
    [:th {:class "shrink-column"} "Version"]
    [:th {:class "shrink-column" :row-span 2} "Change num"]
    [:th {:class "shrink-column" :row-span 2} "Time"]
    [:th {:class "shrink-column" :row-span 2} "Type"]
    [:th {:class "shrink-column" :row-span 2} "Value"]
    [:th {:class "shrink-column"} "Compare"]]
   [table-filter-section :DUMMY filters]])



(defn get-label-text
  [extended-mode?]
  [:div {:class "ui blue label"}
   (if extended-mode? "extended mode" "minified mode")])



(defn show-extended-table
  [preferences items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header-extended :DUMMY filters]
   [:tbody
    (for [itm items]
      (create-table-extended preferences (val itm)))]])



(defn show-minified-table
  [preferences items filters]
  [:table {:class "ui selectable celled fixed table"}
   [table-header :DUMMY filters]
   [:tbody
    (for [itm items]
      (create-minified-table preferences (val itm)))]])



(defn create-config-table
  [preferences extended-mode? real-filters items]
  [:div {:class "sixteen wide column config-table-scroll"}
   [:div {:class "ui grid"}
    (if (true? extended-mode?)
      (show-extended-table preferences items real-filters)
      (show-minified-table preferences items real-filters))]])



(defn show-entry-window [preferences item-params item-data]
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
          (utils/as-label (colourize-label preferences  env) env))]]

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
       [:td {:class "center aligned collapsing"} (utils/as-label (get item-params :content-type))]]
      [:tr
       [:td {:class "center aligned collapsing" :col-span "2"}
        [:button {:class "ui grey button right floated left aligned" :on-click #(ctl/close-new-entry-panel! %)} "Close"]]]]]]

   [:div {:class "ten wide column"}
    [:div {:class "ui raised segment"}
     [:a {:class "ui blue ribbon label" :on-click #(ctl/copy-to-clipboard!)} "Copy to clipboard"]
     [:a {:class "ui grey right ribbon label" :on-click #(ctl/discard-changes! item-data)} "Discard changes"]
     [:div  {:class "overflow-class"}
      [:div {:id "jsEditor"} item-data]]]]
   [ctl/highlight-code-block true (get item-params :content-type)]
   [:div {:class "three wide column"}]

   ;;-----------------------------------------
   [:div {:class "three wide column"}]
   [:div {:class "ten wide column"}]
   [:div {:class "three wide column"}]])

(defn new-entry-details-window
  [_ new-entry]
  [:form {:class "ui form" :on-submit #(ctl/add-config-entry! %1)}
  [:div {:class "ui grid" :style {:padding "16px"}}
   [:div {:class "three wide column"}
     [:div {:class "edit-background"}
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
                    :on-change #(ctl/upload-file %)}]

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
      [:div {:class "four wide column"}
       [:button {:class "ui primary button"} "Save"]]
      [:div {:class "four wide column"}]
      [:div {:class "four wide column "}
       [:button {:class "ui grey button right floated left aligned" :on-click #(ctl/close-new-entry-panel! %)} "Close"]]
      [:div {:class "two wide column"}]]]]

   [:div {:class "ten wide column"}
    [:div {:class "ui raised segment"}
     [:a {:class "ui blue ribbon label" :on-click #(ctl/copy-to-clipboard!)} "Copy to clipboard"]
     [:a {:class "ui grey right ribbon label" :on-click #(ctl/discard-changes! (get new-entry :val))} "Discard changes"]
     [:div  {:class "overflow-class"}
      [:div {:id "jsEditor"} (get new-entry :val)]
      [ctl/highlight-code-block false (get new-entry :type)]]]]
   [:div {:class "three wide column"}]
   ;;-----------------------------------------
   [:div {:class "three wide column"}]
   [:div {:class "ten wide column"}]
   [:div {:class "three wide column"}]]])

(defn compare-entry-details-window
  [_ compare-items ]
  [:form {:class "ui form" :on-submit #(ctl/add-config-entry! %1)}
  [:div {:class "ui grid" :style {:padding "16px"}}
   [:div {:class "three wide column"}
     [:div {:class "edit-background"}
     [:div {:class "ui grid"}
      [:div {:class "two wide column"}]
      [:div {:class "twelve wide column"}
       [:div {:class "row onecfg-filter-block"}]
       [:div {:class "row onecfg-filter-block"}]
       [:div {:class "row onecfg-filter-block"}]
       [:div {:class "row onecfg-filter-block"}]
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
                    :on-change #(ctl/upload-file %)}]

           [:label {:for "file-input" :class "ui mini blue button"}
            [:i {:class "ui upload icon"}] "Upload"]]
          [:td]
          [:td]]]]]
      [:div {:class "two wide column"}]
      ;;;-------------------------------------------------
      [:div {:class "two wide column"}]
      [:div {:class "four wide column"}
       [:button {:class "ui primary button"} "Save"]]
      [:div {:class "four wide column"}]
      [:div {:class "four wide column "}
       [:button {:class "ui grey button right floated left aligned" :on-click #(ctl/close-new-entry-panel! %)} "Close"]]
      [:div {:class "two wide column"}]]]]

   [:div {:class "ten wide column"}
    [:div {:class "ui raised segment"}
     [:div  {:class "overflow-class"}
      [:div {:id "acediff"}]
      [ctl/compare-code-block (get compare-items :entries)]
      ]]]
   [:div {:class "three wide column"}]
   ;;-----------------------------------------
   [:div {:class "three wide column"}]
   [:div {:class "ten wide column"}]
   [:div {:class "three wide column"}]]])

(defn entry-button-text [mode]
  (cond
    (= :listing mode)  "New Entry"
    (= :new-entry-mode mode) "Edit Entry"
    (= :show-entry-mode mode) "Edit Entry"
    (= :edit-entry-mode mode) "New Entry"
    (= :compare-entry-mode mode) "New Entry"
    :else (println (str "unknown mode : " mode))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| M A I N   P A G E |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main-page
  [current-state]
  [:div
   [:div {:class "sticky-nav-bar"}
    [:div {:class "ui secondary menu"}
     [:div {:class "item"}
      [:div {:class (get current-state :entry-management-button-style) :on-click #(ctl/config-entry-management-panel! (get current-state :client-mode))}
       [entry-button-text (get current-state :client-mode)]]]
     [:div {:class "item"}
      [:div {:class    (if (= (get-in current-state [:selected :counter]) 2)
                         "ui inverted button"
                         "ui inverted disabled button") :on-click #(ctl/compare-selected-items (get current-state :selected)) } "Compare" ]]
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
     [create-config-table
      (:preferences current-state)
      (:extended-mode? current-state)
      (:filters current-state)
      (group-by :key
                (utils/filter-entries
                 (get current-state :filters)
                 (get current-state :entries)))]

     (let [mode (:client-mode current-state )
           item-params (:item-params current-state)
           item-data (:item-data current-state)]
       (cond
         (= :listing mode) [:div {:class "modal"}]
         (= :new-entry-mode mode)  [:div {:class "modal show-modal"}
                                    [new-entry-details-window :DUMMY (get current-state :new-entry)
                                     ]]
         (= :edit-entry-mode mode)  [:div {:class "modal show-modal"}
                                     [:div {:class "hide-element"}
                                      [ctl/copy-data-to-new-entry! item-params item-data]]
                                    [new-entry-details-window :DUMMY (get current-state :new-entry)
                                     ]]
         (= :show-entry-mode mode) [:div {:class "modal show-modal"}
                                    [show-entry-window
                                     (:preferences current-state)
                                     item-params
                                     item-data]]
         (= :compare-entry-mode)  [:div {:class "modal show-modal"}
                                   [compare-entry-details-window :DUMMY (get current-state :selected)]]
         :else [:div {:class "modal"}]))]]
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
  (ctl/get-preferences!)
  (ctl/get-all-configs!)
  (ctl/get-version!)
  (reagent/render [app-root]
                  (. js/document (getElementById "app")))
  (ctl/hide-element "loader"))
