(ns whiteboard.tools.text.events
  (:require [whiteboard.helpers.stream :refer [init-stream-item]]
            [whiteboard.helpers.shared :refer [conj-in]]
            [re-frame.core :refer [reg-event-fx reg-event-db]]))

(reg-event-fx
 ::text-change
 (fn [{:keys [db]} [_ val]]
   {:db (assoc-in db [:active-stream :text] val)}))

(reg-event-db
 ::text-blur
 (fn [db _]
   (dissoc db :active-stream)))


(reg-event-fx
 ::try-persist-text-stream
 (fn [{:keys [db]} [_ _]]
   (let [active-stream (:active-stream db)]
    ;;  (cljs.pprint/pprint {:active-stream active-stream})
    ;;  (cljs.pprint/pprint (get-in db [:stream active-stream :text]))
     (if-not (get-in db [:stream active-stream :text])
       {:db (-> db (dissoc :active-stream) (update :stream #(dissoc % active-stream)))}
       {:db (-> db
                (assoc-in [:stream (get-in db [:active-stream :id])] (:active-stream db))
                (conj-in [:stream-order] (get-in db [:active-stream :id]))
                (dissoc :active-stream :shadow-shape))}))))


(reg-event-fx
 ::input-blur
 (fn [{:keys [db]} [_ _]]
   (let [item (:active-stream db)]
     {:db (dissoc db :active-stream :shadow-shape)
      :dispatch [::try-persist-text-stream item]})))

;; todo rewrite this thing
(defn mouse-down-fx [db xy]
 ; mouse-down
  (let [item-id (:next-item-id db)
        item (-> item-id
                 (init-stream-item)
                 (assoc :points [xy])
                 (assoc :config (-> db :draw :config))
                 (assoc-in [:config :moving] true))
        cur-item (:active-stream db)]

    (if cur-item
      {:dispatch [::try-persist-text-stream]}
      {:db (-> db
               (assoc
                :next-item-id (+ 1 item-id))
               (assoc-in
                [:stream item-id]
                item))})))

(defn mouse-move-fx [db xy]
; mouse-move
  (when-let [item-id (:active-stream db)]
    {:db (assoc-in db [:stream item-id :points] [xy])}))


(defn mouse-up-fx [db event])
  ;; (when-let [item-id (:active-stream db)
  ;;            item (-> db (:stream) item-id)]
  ;;   {:db (update-in db [:stream item-id :config] dissoc :moving)}))