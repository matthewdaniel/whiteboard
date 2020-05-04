(ns whiteboard.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :refer [dispatch-sync clear-subscription-cache!]]
   [whiteboard.events :as events]
   [whiteboard.keyboardfx :as keyboardfx]
   [whiteboard.routes :as routes]
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

(defn init []
  (routes/app-routes)
  (dispatch-sync [::events/initialize-db])
  (dispatch-sync [::keyboardfx/initialize-shortcuts])
  (dev-setup)
  (mount-root))
