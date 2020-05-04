(ns whiteboard.helpers.shared
  (:require 
   [camel-snake-kebab.core :as csk]
   [re-frame.core :as rf]))

(def <sub (comp deref rf/subscribe))
(def >evt rf/dispatch)
(def >value-evt (fn [[evt js-event]]
                  (rf/dispatch [evt (-> js-event .-target .-value)])))

(defn event-dispatcher [name] #(->> % (conj [name]) (rf/dispatch)))

(defn js-to-clj [obj]
  (->> obj 
       (js-keys) 
       (js->clj)
       (map #(conj [(csk/->kebab-case-keyword %)] (aget obj %)))
       (into {})))
