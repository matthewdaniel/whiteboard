(ns whiteboard.core
  (:require
   [reagent.core :as reagent]
   [clojure.spec.alpha :as s]
   [re-frame.core :refer [dispatch-sync clear-subscription-cache!]]
   [whiteboard.events :as events]
   [whiteboard.helpers.connection :as con]
   [d3-interpolate :refer [interpolateObject]]
   [d3-shape :refer [line curveCatmullRom]]
   [clojure.string :as str]
   [whiteboard.keyboardfx :as keyboardfx]
   [whiteboard.routes :as routes]
   [whiteboard.helpers.firebase :as firebase]
   [whiteboard.views :as views]
   [whiteboard.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))
(s/check-asserts true)

(defn init []
  (routes/app-routes)
  (dispatch-sync [::events/initialize-db])
  (dispatch-sync [::keyboardfx/initialize-shortcuts])
  (con/init "123") ; todo - generate a workspace id instead of using 123
  (firebase/init)
  (dev-setup)
  (mount-root))