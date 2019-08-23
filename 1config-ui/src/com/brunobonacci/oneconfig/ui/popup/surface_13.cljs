(ns com.brunobonacci.oneconfig.ui.popup.surface-13
  (:require
    [com.brunobonacci.oneconfig.ui.comm :as comm]
    [cljs.pprint :as pp]))

(defn dimmer [app-state]
  [:div { :style    {:height "100%" :width  "100%"}
         :on-click (fn [] (swap! app-state assoc :page-key :surface-13))}])

(defn modal [app-state]
  [:div {:class "ui grid" :style {:padding "16px"}}
   [:div {:class "four wide column"}
    [:div {:class "modal-content"}
     [:table {:class "ui celled striped table"}
      [:thead
       [:tr
        [:th {:class "center aligned collapsing" :col-span "2"}(get-in @app-state [:item-params :key]) ]]]
      [:tbody
       [:tr
        [:td {:class "center aligned collapsing"} "Environment"]
        [:td {:class "center aligned collapsing"}
         (let [env (get-in @app-state [:item-params :env])]
           (comm/as-label (comm/colourize-label env) env))
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Version"]
        [:td {:class "center aligned collapsing"} (get-in @app-state [:item-params :version])  ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Change num"]
        [:td {:class "center aligned collapsing"} (get-in @app-state [:item-params :change-num])
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Time"]
        [:td {:class "center aligned collapsing"} (comm/parse-date (get-in @app-state [:item-params :change-num]))
         ]]
       [:tr
        [:td {:class "center aligned collapsing"} "Type"]
        [:td {:class "center aligned collapsing"} (comm/as-label (get-in @app-state [:item-params :content-type]))]]
       ]]

     ]
    ]
   [:div {:class "ten wide column"}]
   [:div {:class "two wide column"}]
   [:div {:class "three wide column"}]
   [:div {:class "twelve wide column"}
    [:div {:class "ui raised segment"}
     [:a {:class "ui blue ribbon label"} "Value"]
     (comm/as-code (get @app-state :item-data))
     ]
    ]
   [:div {:class "one wide column"}]
   ]
  )

(def components
  {:dimmer {:surface-13-dimmer dimmer}
   :modal {:surface-13-modal modal}
   })

(def surface-init
  {
   :dimmer        {:key :surface-13-dimmer}
   :modal         {:active? false
                   :width 300
                   :height 500
                   }})

(def surfaces
  {:surface-13 surface-init
   :surface-13-modal (-> surface-init
                         (assoc-in [:modal :key] :surface-13-modal)
                         (assoc-in [:modal :active?] true))})