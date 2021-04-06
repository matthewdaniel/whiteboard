(ns whiteboard.helpers.connection
  (:require [re-frame.core :refer [reg-sub subscribe reg-event-db reg-event-fx reg-fx]]
            [whiteboard.helpers.shared :refer [<sub >evt]]
            ;; [whiteboard.helpers.firebase :refer [on]]
            ))

;; todo: open up webrtc connections to share drawing with participatns
(def ice
  {:iceServers [{:urls ["stun:stun1.l.google.com:19302", "stun:stun2.l.google.com:19302"]}]
   :iceCandidatePoolSize 10})

(reg-event-db
 ::calls-received
 (fn [db [_ val]]
   db
   db))

(defn init [id])
  ;; (on [:calls id] #(>evt [::calls-received %])))
