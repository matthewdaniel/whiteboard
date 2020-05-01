(ns whiteboard.helpers.localstore
  (:require [cljs.reader :refer [read-string]]))

(defn get-key [key]
  (-> key
      (js/window.localStorage.getItem)
      (read-string)))

(defn set-key [key value]
  (->> value
       (prn-str)
       (js/window.localStorage.setItem key)))