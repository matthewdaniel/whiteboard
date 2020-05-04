(ns whiteboard.views
  (:require
   [luxon :refer [DateTime]]
   [whiteboard.helpers.shared :refer [<sub >evt >value-evt]]
   [whiteboard.subs :as subs]
   [whiteboard.events :as events]
   [reagent.core :refer [create-class dom-node]]))

(defn main-canvas []
  (create-class 
   {:component-did-mount #(->> % (dom-node) (conj [::events/initialize-canvas]) (>evt))
    :reagent-render (fn [] [:canvas#canvas])}))

(defn snap [{:keys [time id preview]}] 
  [:div.snapshot {:key id :class (when (= id (<sub [::subs/preview-mode])) "active") :on-click #(>evt [::events/preview-snap id])}
   [:img.snap-preview {:src preview}]
   [:i.fas.fa-history.restore-snap {:on-click #(>evt [::events/restore-snapshot])}]
   [:div.description (.toLocaleString time (.-DATETIME_SHORT_WITH_SECONDS DateTime))]])

(defn snapshots []
    [:div.snapshot-panel (doall (map snap (<sub [::subs/snapshots])))])

(defn right-panel []
  [:div#right-panel
   [:div 
    [:button {:on-click #(>evt [::events/undo])}  "undo"]
    [:button {:on-click #(>evt [::events/snapshot])}  "snapshot"]
    [:button {:on-click #(>evt [::events/clear-stream])} "clear"]]
   [snapshots]])

(defn color-button [color] 
  [:button.color 
   {:key color
    :style {:color color :border-color color}
    :on-click #(>evt [::events/change-color color])
    } color])

(defn left-panel []
  [:div.left-panel
   [:div.input-row 
    [:span "Line Width"] 
    [:input {:type "number" 
             :on-change #(>value-evt [::events/change-draw-line-width %])
             :value (<sub [::subs/draw-line-width])}]]
   [:div.input-row
    [:span "Color"]
    [:div.color-buttons
     (map color-button '("#f7412d" "#47b04b" "#1194f6" "#ffc200" "#9d1bb2"))]]])

(defn preview-overlay []
    
  [:div.preview {:class (when-not (<sub [::subs/preview-mode]) "hidden")}
   [:div.preview-message
    [:div.preview-text "Preview Mode"]
    [:div.preview-close {:on-click #(>evt [::events/close-preview])}
     [:i.far.fa-times-circle]]]])

(defn main-panel []
  [:div#main
   [left-panel]
   [:div#canvas-container 
    [main-canvas]
    [preview-overlay]]
   [right-panel]])