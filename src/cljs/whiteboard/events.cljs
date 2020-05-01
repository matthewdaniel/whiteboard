(ns whiteboard.events
  (:require
   [whiteboard.helpers.localstore :as  store]
   [re-frame.core :as rf]
   [whiteboard.db :as db]
   ))

(defn get-user-id []
  (let [user-id? (store/get-key :user-id)
        user-id (or user-id? (random-uuid))]
    (store/set-key :user-id user-id)
    user-id))

; make a event dispatcher apropriate for js events
(defn event-dispatcher [name] #(->> % (conj [name]) (rf/dispatch)))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   (assoc db/default-db
          :user-id 
          (get-user-id)
          :canvas
          (.getElementById js/document "canvas"))))

(rf/reg-event-fx
 ::initialize-canvas
 (fn [{:keys [db]} [_ canvas]]
   (set! (.-width canvas) (- (.-clientWidth js/document.body) 200))
   (set! (.-height canvas) (.-clientHeight js/document.body))

   (. canvas addEventListener "mousedown" (event-dispatcher ::mouse-down) false)
   (. canvas addEventListener "mousemove" (event-dispatcher ::mouse-move) false)
   (. canvas addEventListener "mouseup" (event-dispatcher ::mouse-up) false)
  ;  (. canvas addEventListener "mouseout" (event-dispatcher ::mouse-up) false)
   {:db (assoc db :canvas canvas)}))


(rf/reg-event-db
 ::set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(rf/reg-event-db
 ::mouse-down
 (fn [db [_ event]]
   (let [ctx (.getContext (:canvas db) "2d")]
     (.beginPath ctx)
     (set! (.-strokeStyle ctx) "black")
     (set! (.-lineWidth ctx) 1)
     (assoc db :interaction-stream {:ctx ctx :points [{:x (.-offsetX event) :y (.-offsetY event)}]}))))

(defn conj-in [map path val] (update-in map path #(-> % (vec) (conj val))))

(defn event-xy [event] {:x (.-offsetX event) :y (.-offsetY event)})
(defn redraw-stream [stream] (doall (map (fn [{:keys [ctx]}] (.stroke ctx)) stream)))


(rf/reg-event-db
 ::undo
 (fn [db [_ event]]
   (cljs.pprint/pprint :undo)
   (let [last-shape (last (:stream db))
         {:keys [ctx]} last-shape
         {:keys [canvas]} db
         new-stream (drop-last (:stream db))]
     (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
    ;  (clearRect (0,0, 1336, 373))
     ; todo: should something watch the stream rather than changers having to notify?
     (cljs.pprint/pprint [:new-stream (count new-stream)])
     (redraw-stream new-stream)
     (assoc db :stream new-stream)))
     )


; should only be called if mouse actually moved in the point stream
(rf/reg-event-db
 ::free-draw-new-xy
 (fn [db [_ event]]
   (cljs.pprint/pprint :free-draw-mouse-move)
   (let [xy (event-xy event)
         new-db (conj-in db [:interaction-stream :points] xy)
         {:keys [ctx points]} (:interaction-stream new-db)
         [{x1 :x y1 :y} {x2 :x y2 :y}] (take-last 2 points)]
     ; as long as we have ctx and a new point to move from then graph it
     (when (and ctx (not= [x1 y1] [x2 y2]) x2)
       (.moveTo ctx x1 y1)
       (.lineTo ctx x2 y2)
       (.stroke ctx))
     new-db)))

(rf/reg-event-fx
 ::mouse-move
 (fn [{:keys [db]} [_ event]]
   (when-let [stream (:interaction-stream db)] 
     (let [{:keys [ctx points]} stream
           point-is-new (not= (last points) (event-xy event))]
       (when (and ctx point-is-new) (rf/dispatch [::free-draw-new-xy event]))))))  
   
(rf/reg-event-db
 ::mouse-up
 (fn [db [_ event]]
   (let [{:keys [points ctx]} (:interaction-stream db)]
     (-> db
         (dissoc :interaction-stream)
         (conj-in [:stream] {:ctx ctx :points points})))))