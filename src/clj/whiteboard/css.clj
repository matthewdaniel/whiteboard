(ns whiteboard.css
  (:require
   [garden.def :refer [defstyles]]))


(defstyles screen
  [:html {:height "100%"}]
  [:div#app {:height "100%"}]
  [:div.welcome
   {:position "absolute"
    :z-index -2
    :top 0
    :left 0
    :right 0
    :bottom 0
    :display "flex"
    :justify-content "center"
    :align-items "center"
    :font-size "80px"
    :color "#8888ff88"
    :font-family "cursive"}]
  [:body
   {:color "black"
    :overflow "hidden"
    :height "100%"
    :padding 0
    :margin 0
    :background-image "radial-gradient(#ddd 3%, #fff 5%)"
    :background-position "0 0"
    :background-size "49px 49px"
    :width "100%"}
   [:&.dark-mode {:background-image "radial-gradient(#aca 2%, #333 5%)"}]]
  [:div.svg-canvas-wrapper {:width "100%" :height "100%"}]
  [:div.canvas-overlay {:width "100%" :height "100%" :z-index 1}]
  [:svg.svg-canvas
   {:width "100%"
    :height "100%"
    :position "absolute"
    :z-index -1}]
  [:canvas]
  [:div#app {:height "100%"}]
  [:div.snaps
   {:position "absolute"
    :display "flex"
    :flex-direction "column"
    :bottom 0
    :max-height "90%"
    :user-select "none"
    :right "20px"
    :background "#eee"
    :border-top-right-radius "5px"
    :border-top-left-radius "5px"
    :border "1px solid #ddd"
    :border-bottom-width 0
    :padding "10px"}
   [:.tray-header
    {:cursor "pointer"
     :display "flex"
     :justify-content "space-between"
     :position "relative"}
    [:div.snap-animation
     {:position "absolute"}
     [:&.start
      {:right "15px"
       :bottom "90px"
       :font-size "70px"
       :opacity 0.5}]
     [:&.finish
      {:transition "800ms"
       :bottom "-10px"
       :opacity 0
       :right "30px"
       :font-size "30px"}]]
    [:.expand
     {:margin-left "10px" :padding-right "20px"}
     [:.ico {:position "absolute" :top "5px" :right "13px" :color "#999"}]]]
   [:img.snap-img
    {:background-color "white"
     :width "150px"
     :height "150px"}]]
  [:div.action-menu
   [:&.action
    [:.title {:display "none"}]]
   [:div.button
    {:font-size "22px"
     :cursor :pointer
     :border-radius "10px"
     :margin "2px"
     :padding "8px"}
    [:&:hover {:background-color "#00000011"}]]
   [:div.action-panel-container
    {:position "relative"}
    [:div.action-panel
     {:position "absolute"
      :visibility "hidden"
      :display "flex"
      :flex-direction "column"
      :align-items "center"
      :left "90px"
      :top "-55px"
      :padding "15px"
      :background-color "#eee"
      :color "black"
      :opacity 0
      :transition "left 150ms linear, opacity 50ms ease-out"
      :border-radius "10px"}
     [:&.active
      {:visibility "inherit"
       :left "60px"
       :opacity 1}]]]]

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
       :border-bottom-width "0"}
      [:div.preview-close {:cursor "pointer"}
       [:i {:font-size "22px" :line-height "20px" :margin-left "20px"}]]]]]
   [:div#right-panel
    {:width "200px"
     :height "100%"
     :box-sizing "border-box"
     :overflow "hidden"
     :display "flex"
     :flex-direction "column"
     :background-color "#eee"
     :padding "8px"
     :-webkit-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :-moz-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"}
    [:button {:margin "px"}]]]
  
  [:&.dark-mode
   [:div#right-panel {:background-color "darkslategrey !important" :color "antiquewhite" :box-shadow "none !important" :border-left-color "#aaa !important"}]
   [:div.left-panel {:background-color "darkslategrey" :color "antiquewhite"}]]

  [:div.snapshot-panel
   {:display "flex" :padding-top "10px" :flex-direction "column" :user-select "none" 
    :overflow "auto"}
   [:div.snapshot
    {:display "flex"
     :flex-direction "column"
     :flex 1
     :font-size "13px"
     :cursor "zoom-in"
     :position "relative"
     :margin-bottom "5px"
     :align-items "center"
     }
    ;; [:&.active {:border "2px solid cornflowerblue"}]
    [:div.time
     {:position "absolute"
      :bottom "2px"
      :right "2px"}]
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
   {:position "absolute"
    :z-index 2
    :left "10px"
    :top "10px"
    :border-radius "10px"
    :background-color "#eee"
    :border "1px solid #dedede"
    :-webkit-box-shadow "5px 5px 30px -20px #919191"
    :box-shadow "5px 5px 30px -20px #919191"
    :padding "8px"
    :display "flex"
    :flex-direction "column"}]
  [:div.misc-actions
   {:display "flex" :flex-direction "column"}
   [:div.action-item
    {:display "flex"
     :align-items "center"
     :padding "10px"
     :border-radius "3px"
     :cursor "pointer"}
    [:.ico {:width "25px"}]
    [:&:hover {:background-color "#00000011"}]
    [:div.desc
     {:display "flex"
      :justify-content "center"
      :flex-direction "column"
      :align-items "flex-start"
      :margin-left "5px"
      :line-height "10px"
      :font-size "13px" :white-space "nowrap"}
     [:.key-combo
      {:font-size "8px" :white-space "nowrap" :margin-top "4px" :display "flex" :align-items "center"}]]]]

  [:div.tools
   {:display "flex"}
   [:div
    {:margin "10px 20px"
     :cursor "pointer"
     :font-size "20px"}]]
  [:div.color-buttons
   {:display "flex"
    :padding "8px"}
   [:button.color
    {:border "1px solid" :margin "10px 20px" :width "20px" :padding 0 :height "20px" :cursor "pointer" :border-radius "100px"}
    [:&:hover {:-webkit-box-shadow "2px ​2px 4px -1px #555"
               :box-shadow "2px ​2px 4px -1px #555"}]]]
  [:svg.shadow-shape
   {:height "100%"
    :width "100%"
    ;; :float "right"
    :position "absolute"
    :z-index -1}]
  [:input#text-shadow-shape
   {:outline "none"
    :border "2px dotted aqua"
    :position "absolute"
    :z-index -1
    :padding "5px"
    :margin-top "-10px"
    :margin-left "-10px"}])
