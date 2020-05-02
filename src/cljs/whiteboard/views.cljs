(ns whiteboard.views
  (:require
   [re-frame.core :as rf]
   [whiteboard.subs :as subs]
   [whiteboard.events :as events]
   [reagent.core :refer [create-class dom-node]]
   ))


;; home

(defn home-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [:h1 (str "Hello from " @name ". This is the Home Page.")]

     [:div
      [:a {:href "#/about"}
       "go to About Page"]]
     ]))


;; about

(defn about-panel []
  [:div
   [:h1 "This is the About Page."]

   [:div
    [:a {:href "#/"}
     "go to Home Page"]]])


;; main

(defn- panels [panel-name]
  (case panel-name
    :home-panel [home-panel]
    :about-panel [about-panel]
    [:div]))

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-canvas []
  (create-class 
   {
    :component-did-mount
    #(->> % (dom-node) (conj [::events/initialize-canvas]) (rf/dispatch))
    :reagent-render (fn [] [:canvas {:id "canvas" :width 800 :height 800}])}))

(defn snap [{:keys [time]}] [:span (str time)])

(defn snapshots []
  (let [snaps (rf/subscribe [::subs/snapshots])]
    [:div.snap-panel (map snap @snaps)]))

(defn right-panel []
    ;; (cljs.pprint/pprint [:stream @stream])
  [:div#right-panel
   [:div 
    [:button {:on-click #(rf/dispatch [::events/undo])}  "undo"]
    [:button {:on-click #(rf/dispatch [::events/snapshot])}  "snapshot"]
    [:button {:on-click #(rf/dispatch [::events/clear-stream])} "clear"]]
   [snapshots]])

(defn main-panel []
  [:div#main
   [:div#canvas-container
    [main-canvas]]
   [right-panel]])