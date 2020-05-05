(ns whiteboard.keyboardfx
  (:require
   [whiteboard.events :as event]
   [whiteboard.helpers.shared :refer [js-to-clj event-dispatcher >evt]]
   [luxon :refer [DateTime]]
   [clojure.string :as str]
   [whiteboard.shapefx :as shapefx]
   [re-frame.core :refer [reg-fx dispatch reg-event-fx]]
   [whiteboard.db :as db]))

(reg-fx
 ::ctrl-z
 (fn [_]
   (>evt [::event/undo])))

(reg-event-fx
 ::keydown
 (fn [{:keys [event db]}]
   (let [[_ keydown-js] event
         {:keys [key ctrl-key meta-key]} (js-to-clj keydown-js)
         pressed (keyword (str (when meta-key "meta-") (when ctrl-key "ctrl-") key))
         ret (case pressed
               :ctrl-z {::ctrl-z nil}
               :meta-z {::ctrl-z nil}
               {})]
     ; if we have a handler then stop default behavior
     (when (not= ret {}) (.preventDefault keydown-js))
    ;;  (when (= :text (get-in db [:active-stream :config :tool]))
       (cljs.pprint/pprint [:keydown key (get-in db [:active-stream :config])])
    ;;  )

     ret)))


(reg-event-fx
 ::initialize-shortcuts
 (fn [_ _]
   (.addEventListener js/document "keydown" (event-dispatcher ::keydown) false)))

