(ns whiteboard.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [whiteboard.events :as events]
   [whiteboard.routes :as routes]
   [whiteboard.views :as views]
   [whiteboard.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/initialize-canvas])
  (dev-setup)
  (mount-root))
