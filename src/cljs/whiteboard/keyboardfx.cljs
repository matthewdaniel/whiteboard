(ns whiteboard.keyboardfx
  (:require
   [whiteboard.events :as events]
   [whiteboard.helpers.shared :refer [js-to-clj event-dispatcher >evt]]
   [luxon :refer [DateTime]]
   [clojure.string :as str]
   [re-frame.core :refer [reg-fx dispatch reg-event-fx reg-event-db]]
   [whiteboard.db :as db]))

(reg-fx
 ::ctrl-z
 (fn [_]
   (>evt [::events/undo])))

(reg-event-db ::ctrl-down #(assoc % :ctrl-hold true))

(reg-event-fx
 ::keydown
 (fn [{:keys [event db]}]
   (let [[_ keydown-js] event
         {:keys [key ctrl-key meta-key]} (js-to-clj keydown-js)
         pressed (keyword (str (when meta-key "meta-") (when ctrl-key "ctrl-") key))
         evt (case pressed
               :ctrl-z {::ctrl-z nil}
               :meta-z {::ctrl-z nil}
               :ctrl-Control {:dispatch [::ctrl-down]}
               :Escape {:dispatch [::esc]}
               {})]
     (-> {} (merge evt)))))

(reg-event-fx
 ::esc
(fn [{:keys [event db]}]
   (let [active-stream-id (:active-stream db)
         next-db (if-not active-stream-id
       db
       (-> db
         (update :stream dissoc active-stream-id)
         (dissoc :active-stream)
         ))
         ]
     {:db db
      :dispatch [::events/set-visible-menu nil]}
   )))

(reg-event-fx
 ::keyup
 (fn [{:keys [event db]}]
   (let [[_ keydown-js] event
         {:keys [key ctrl-key]} (js-to-clj keydown-js)]
     (when-not ctrl-key {:db (dissoc db :ctrl-hold)}))))

(reg-event-fx
 ::initialize-shortcuts
 (fn [_ _]
   (.addEventListener js/document "keydown" (event-dispatcher ::keydown) false)
   (.addEventListener js/document "keyup" (event-dispatcher ::keyup) false)))


