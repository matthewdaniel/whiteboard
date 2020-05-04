(ns whiteboard.css
  (:require 
   [garden.def :refer [defstyles]]))


(defstyles screen
  [:html {:height "100%"}]
  [:div#app {:height "100%"}]
  [:body
   {:color "black" 
    :height "100%"
    :padding 0 
    :margin 0
    :background-image "radial-gradient(#ddd 3%, #fff 5%)"
    :background-position "0 0"
    :background-size "49px 49px"
    :width "100%"}
   [:&.dark-mode {:background-image "radial-gradient(#aca 2%, #333 5%)"}]]
  [:canvas]
  [:div#app {:height "100%"}]
  [:div#main {:display "flex" :height "100%"}
   [:div#canvas-container {:flex 1 :position "relative"}
    [:div.preview
     {:position "absolute"
      :cursor "not-allowed"
      :top 0 :bottom 0 :left 0 :right 0
      :background "repeating-linear-gradient(-55deg, rgba(10,10,10, .1), rgba(10,10,10, .1) 10px, rgba(22,22,22, .05) 10px ,rgba(22,22,22, .05) 20px)"}
     [:&.hidden {:display "none"}]
     [:div.preview-message 
      {:position "absolute"
       :right "40px"
       :bottom 0
       :padding "15px 15px 5px 15px"
       :background "#fff"
       :border-top-right-radius "10px"
       :border-top-left-radius "10px"
       :display "flex"
       :border "1px solid cornflowerblue"
       :border-bottom-width "0"}
      [:div.preview-close { :cursor "pointer" }
       [:i {:font-size "22px" :line-height "20px" :margin-left "20px"}]]]] ]
   [:div#right-panel 
    {:width "200px"
     :height "100%"
     :box-sizing "border-box"
     :overflow "hidden"
     :display "flex"
     :flex-direction "column"
     :background-color "#eee"
     :padding "8px"
     :border-left "1px solid cornflowerblue"
     :-webkit-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :-moz-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"}]]
  [:&.dark-mode 
   [:div#right-panel {:background-color "darkslategrey !important" :color "antiquewhite" :box-shadow "none !important" :border-left-color "#aaa !important"}] 
   [:div.left-panel {:background-color "darkslategrey" :color "antiquewhite"}]]
  [:div.snapshot-panel 
   {:display "flex" :padding-top "10px" :flex-direction "column" :user-select "none" :max-height "100%" :overflow "auto"}
   
   [:div.snapshot
    {:display "flex"
     :flex-direction "column"
     :flex 1
     :font-size "13px"
     :cursor "zoom-in"
     :position "relative"
     :margin-bottom "5px"
     :align-items "center"}
    [:&.active {:border "2px solid cornflowerblue"}]
    [:i.restore-snap {:display "none"}]
    [:&:hover>i.restore-snap
     {:position "absolute"
      :display "inline-block"
      :top "5px"
      :right "5px"
      :cursor "pointer"
      :padding "4px"}]
    [:img.snap-preview
     {:width "100%"
      :background-color "white"}]
    [:div.description
     {:background-color "white"
      :width "100%"
      :text-align "left"}]]]
  [:div.left-panel 
   {:width "150px" 
    :background-color "wheat"
    :padding "8px"
    :display "flex"
    :flex-direction "column"}]
  [:div.color-buttons
   {:display "flex"
    :border "1px solid black"
    :flex-direction "column"
    :padding "8px"
    :background-color "white"}
   [:button.color
    {:border "1px solid" :padding "5px" :margin "5px" :cursor "pointer"}]]
  )
