(ns whiteboard.tools.oval.events
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
        (assoc :next-item-id (+ 1 item-id))
        (assoc :active-stream item-id)
        (#(assoc
           {}
           :db %
           )))))

(defn to-max [max p1 p2]
  (let [diff (js/Math.abs (- p2 p1))
        move-by (js/Math.abs (- max diff))
        new-p2 (if-not (> diff max) p2 (- p2 move-by))
        diff2 (js/Math.abs (- new-p2 p1))
        new-p3 (if-not (> diff2 max) new-p2 (+ p2 move-by))]
    new-p3))

(defn mouse-move-fx [db xy]
; mouse-move
  (when-let [item-id (:active-stream db)]
    (let [{x1 :x y1 :y} (-> db :active-stream :points (first))
          {x2 :x y2 :y} xy
          y-diff (js/Math.abs (- y2 y1))
          x-diff (js/Math.abs (- x2 x1))
          new-x (if (> x-diff y-diff) (to-max y-diff x1 x2) x2)
          new-y (if (> y-diff x-diff) (to-max x-diff y1 y2) y2)
          new-xy (if (:ctrl-hold db) {:x new-x :y new-y} xy)]

      {:db (update-in db [:stream item-id :points] #(-> % (pop) (conj new-xy)))})))
(> 10 0)

(defn mouse-up-fx [db event]
  {:db (dissoc db :active-stream)}
  )