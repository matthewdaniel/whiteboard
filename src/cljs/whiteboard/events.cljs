(ns whiteboard.events
  (:require
   [whiteboard.helpers.localstore :as  store]
   [whiteboard.shapefx :as shapefx]
   [re-frame.core :as rf]
   [whiteboard.db :as db]))

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
   (cljs.pprint/pprint [:initialize-db])
   (assoc db/default-db
          :user-id
          (get-user-id)
          :stream {}
          :stream-order []
          :snapshots []
          :active-stream nil)))

(rf/reg-event-fx
 ::initialize-canvas
 (fn [{:keys [db]} [_ canvas]]
   (set! (.-width canvas) (- (.-clientWidth js/document.body) 200))
   (set! (.-height canvas) (.-clientHeight js/document.body))
   (. canvas addEventListener "mousedown" (event-dispatcher ::mouse-down) false)
   (. canvas addEventListener "mousemove" (event-dispatcher ::mouse-move) false)
   (. canvas addEventListener "mouseup" (event-dispatcher ::mouse-up) false)
   {::shapefx/rebuild-shape-stream (:stream db)}))

(defn conj-in [map path val] (update-in map path #(-> % (vec) (conj val))))
(defn event-xy [event] {:x (.-offsetX event) :y (.-offsetY event)})

(defn free-hand-stream-item-updater [xy stream-item]
  (let [last-point (-> stream-item (:points) (last))]
    (if (= last-point xy)
      stream-item
      (conj-in stream-item [:points] xy))))

(defn update-stream-item [xy stream-item]
  (case (:type stream-item)
    :free-hand (free-hand-stream-item-updater xy stream-item)))

(defn init-stream-item [type id]
  {:id id :type type :points []})

(rf/reg-event-fx
; mouse-down
 ::mouse-down
 (fn [{:keys [db]} [_ event]]
   (-> event
       (event-xy)
       (update-stream-item (init-stream-item :free-hand (random-uuid)))
       ((partial assoc db :active-stream))
       (#(assoc {} :db % ::shapefx/shape-modified (:active-stream %))))))


; if we have an active stream then update it
(rf/reg-event-fx 
; mouse-move
 ::mouse-move
 (fn [{:keys [db]} [_ event]]
   (when-let [active-stream (:active-stream db)]
     (let [newDb (update db :active-stream (partial update-stream-item (event-xy event)))]
       {:db newDb
        ::shapefx/shape-modified (:active-stream newDb)}))))

(rf/reg-event-fx 
; mouse-up
 ::mouse-up
 (fn [{:keys [db]} [_ event]]
   (let [stream-item (:active-stream db)
         item-id (:id stream-item)
         newDb (-> db
                   (assoc :active-stream nil)
                   (conj-in [:stream-order] item-id)
                   (assoc-in [:stream item-id] stream-item))]
     {:db newDb
      ::shapefx/shape-modified (get-in newDb [:stream item-id])})))

(rf/reg-event-fx
 ::undo
 (fn [{:keys [db]} [_ _]]
   (let [last-id (last (:stream-order db))
         new-db (->
                 db
                 (update :stream #(dissoc % last-id))
                 (update :stream-order butlast))]
     {:db new-db
      ::shapefx/rebuild-shape-stream (:stream new-db)})))

(rf/reg-event-db
 ::snapshot
 (fn [db, [_ _]]
   (conj-in db 
            [:snapshots] 
            (merge (select-keys db [:stream :stream-order]) {:time (new js/Date)})
            )))

(rf/reg-event-fx
; clear-stream
 ::clear-stream
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db :stream {} :stream-order [] :snapshots [])
    ::shapefx/rebuild-shape-stream {}}))
