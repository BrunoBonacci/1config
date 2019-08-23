(ns com.brunobonacci.oneconfig.ui.popup.style
  (:require
    [garden.core :refer [css]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util
(defn- px [int]
  (when int
    (str int "px")))

(defn- ->surface-map [opts]
  (let [{:keys [surface-key
                surface-registry]} opts]
    (get surface-registry surface-key)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dimmer
(defn- ->dimmer [opts]
  (let [surface-config (get opts :surface-config)]
    [
     [:.surf-dimmer :.surf-dimmer-dropdown
      {:position         "fixed"
       :background-color "black"
       :top              0
       :bottom           0
       :left             0
       :right            0
       :height           "100%"
       :width            "100%"
       :opacity          0
       :transition       "z-index 0.3s step-end, opacity 0.3s linear"
       :z-index          (get-in surface-config [:z-indicies :dimmer])}]

     [:.surf-dimmer-dropdown
      {:background-color "rgba(0,0,0,0.1)"}]
     ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Modal

(defn- ->modal [opts]
  (let [surface-config   (get opts :surface-config)
        surface-map      (->surface-map opts)
        height           (get-in surface-map [:modal :height])
        width            (get-in surface-map [:modal :width])
        half-width       (/ width 2)
        background-color (get-in surface-map [:modal :background-color] "white")]
    [:.surf-modal
     {:position         "fixed"
      :background-color background-color
      :overflow-y       "auto"
      :top              "-20px" ;; arbitrary
      :left             (str "calc(50% - " (or half-width 0) "px)")
      :height           (when height (px height))
      :width            (px width)
      :opacity          0
      :z-index          (get-in surface-config [:z-indicies :modal])}]
    ))

(defn- ->modal-active [opts]
  (let [surface-config (get opts :surface-config)
        surface-map    (->surface-map opts)
        top            (get-in surface-map [:modal :top] 40)]
    [:&.surf-surface-modal-active

     [:.surf-modal
      {:opacity    1
       :top        (str top "px")
       :z-index    (get-in surface-config [:z-indicies :modal-active])
       :transition "opacity 0.3s linear, top 0.3s linear"}]

     [:.surf-dimmer
      {:opacity    0.5
       :transition "opacity 0.3s linear"
       :z-index    (get-in surface-config [:z-indicies :dimmer-with-modal])}]
     ]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Style
(defn- style [opts]
  [:style
   (css
     [[:body
       {:margin 0}]
      [:.surf-surface
       (->dimmer opts)
       (->modal opts)
       (->modal-active opts)
       ]])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Prepre options
(def indicies
  {
   :dimmer           -1
   :modal            -1
   :dimmer-with-dropdown 4
   :dimmer-with-modal 17
   :modal-active 18
   })

(defn- prepare-opts [opts-raw]
  (let [surface-config         (get opts-raw :surface-config)
        {:keys [style-component
                debug?
                z-indicies]}   surface-config
        surface-config-updated {:style-component (or style-component style)
                                :debug?          (if (nil? debug?) false debug?)
                                :z-indicies      (or z-indicies indicies)}]
    (assoc opts-raw :surface-config surface-config-updated)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Surface

(defn- default-comp [app-state]
  [:div {:style {:height "100%"
                 :width  "100%"
                 :background-color "orange"
                 }}])

(defn- surface [opts-raw]
  (let [opts (prepare-opts opts-raw)
        {:keys [app-state surface-key surface-registry component-registry]} opts
        {:keys [dimmer modal] :as surface-map} (get surface-registry surface-key)

        dimmer-key  (get dimmer :key)
        dimmer-comp (get-in component-registry [:dimmer dimmer-key])

        modal-active?    (get modal :active?)
        modal-key        (get modal :key)
        modal-comp       (get-in component-registry [:modal modal-key] default-comp)
        ]
    [:div.surf-surface
     {:class (when (and modal-comp modal-active?) "surf-surface-modal-active")}

     [:div.surf-dimmer
      [dimmer-comp app-state]]
     [:div.surf-modal
      [modal-comp app-state]]

     [:div.surf-main
      [:div.surf-dimmer-dropdown
       [dimmer-comp app-state]]
      ]
     ]
    ))