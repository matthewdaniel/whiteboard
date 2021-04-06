(ns whiteboard.tools.line.events
  (:require [whiteboard.helpers.stream :refer [init-stream-item]]
            [whiteboard.helpers.shared :refer [conj-in]]))

(defn mouse-down-fx [db xy]
 ; mouse-down
  (let [item-id (:next-item-id db)
        item (init-stream-item item-id)]
    (-> item
        (assoc :points [xy xy])
        (merge {:config (get-in db [:draw :config])})
        ((partial assoc-in db [:stream item-id]))
        (assoc :active-stream item-id)
        (conj-in [:stream-order] item-id)
        (assoc :next-item-id (+ 1 item-id))
        (#(assoc
           {}
           :db %
           )))))

(defn mouse-move-fx [db xy]
; mouse-move
  (when-let [item-id (:active-stream db)]
    {:db (update-in db [:stream item-id :points] #(-> % (pop) (conj xy)))}))


(defn mouse-up-fx [db event]
  {:db (dissoc db :active-stream)}
  )