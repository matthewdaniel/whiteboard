(ns whiteboard.tools.free.events
  (:require
   [whiteboard.helpers.shared :refer [conj-in get-search-param get-href]]
   [whiteboard.helpers.stream :refer [init-stream-item point-distance]]))

(defn mid-point-1d [x1 x2] (+ x1 (/ (- x2 x1) 2)))

(defn mid-point-2d [{x1 :x y1 :y} {x2 :x y2 :y}]
  {:x (js/Math.round (mid-point-1d x1 x2))
   :y (js/Math.round (mid-point-1d y1 y2))})


(defn spread-points
  ([by points] (flatten (spread-points by points [(first points)])))
  ([by points new-points]
   (let [count (count points)
         next (first points)
         dist (point-distance [(last new-points) next])]

     (if (= 1 count)
       (conj [new-points] next)
       (if (> dist by)
         (recur by (rest points) (conj new-points next))
         (recur by (rest points) new-points))))))


(defn add-point-to-stream [{:keys [x y]} stream-item]
  (let [xy {:x x :y y}
        points (concat (:points stream-item) [xy])
        smoothed (spread-points (or (get-search-param (get-href) "spread") 5) points)]
    (assoc stream-item :points smoothed)))


(defn mouse-down-fx [db xy]
 ; mouse-down
  (let [item-id (:next-item-id db)
        config (-> db :draw :config)
        stream (-> (init-stream-item item-id) ((partial add-point-to-stream xy)) (assoc :config config))]
    {:db (-> db
             (update :next-item-id inc)
             (conj-in [:stream-order] item-id)
             (assoc-in [:stream item-id] stream)
             (assoc :active-stream item-id))}))

(defn mouse-move-fx [db xy]

; mouse-move
  (when-let [active-stream-id (:active-stream db)]
    (let [stream (get-in db [:stream active-stream-id])
          last-point (last (:points stream))
          midxy (mid-point-2d last-point xy)
          new-db (update-in db [:stream active-stream-id] (partial add-point-to-stream midxy))]

      {:db new-db})))


(defn mouse-up-fx [db]
; mouse-up
  {:db (dissoc db :active-stream)})