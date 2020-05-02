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
    :width "100%"
    }]
  [:canvas]
  [:div#main {:display "flex" :height "100%"}
   [:div#canvas-container {:flex 1}]
   [:div#right-panel 
    {:width "200px"
     :background-color "#eee"
     :padding "8px"
     :border-left "1px solid cornflowerblue"
     :-webkit-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :-moz-box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"
     :box-shadow "inset 33px 0px 10px -36px rgba(100,149,237,1)"}]]
  )
