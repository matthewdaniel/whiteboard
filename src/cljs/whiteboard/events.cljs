(ns whiteboard.events
  (:require
   [whiteboard.helpers.localstore :as  store]
   [whiteboard.helpers.shared :refer [event-dispatcher]]
   [whiteboard.helpers.const :refer [APP_UUID]]
   [luxon :refer [DateTime]]
   [uuid :refer [v5]]
   [clojure.string :as str]
   [whiteboard.shapefx :as shapefx]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-event-db]]
   [whiteboard.db :as db]))

(defn get-user-id []
  (let [user-id? (store/get-key :user-id)
        user-id (or user-id? (random-uuid))]
    (store/set-key :user-id user-id)
    user-id))

(def default-draw-config 
  {:line-width 2
   :line-color (if (str/includes? (.-className js/document.body) "dark-mode") "#fff" "#000")})

(reg-event-db
 ::change-draw-line-width
 (fn [db [_ new-width]] 
   (assoc-in db [:draw :config :line-width] new-width)))

(reg-event-db
 ::change-color
 (fn [db [_ color]]
   (assoc-in db [:draw :config :line-color] color)))

(reg-event-db
 ::initialize-db
 (fn [_ _]
   (assoc db/default-db
          :user-id
          (get-user-id)
          :stream {}
          :stream-order []
          :snapshots {}
          :dark-mode true
          :draw {:config default-draw-config}
          :active-stream nil)))

(defn get-parent-dims [canvas] 
  (-> 
   canvas 
   (.-parentElement) 
   (#(into {} {:width (.-offsetWidth %) :height (.-offsetHeight %)}))))

(reg-event-fx
 ::initialize-canvas
 (fn [{:keys [db]} [_ canvas]]
   (let [{:keys [width height]} (get-parent-dims canvas)]
     (set! (.-width canvas) width)
     (set! (.-height canvas) height))
   (. canvas addEventListener "mousedown" (event-dispatcher ::mouse-down) false)
   (. canvas addEventListener "mousemove" (event-dispatcher ::mouse-move) false)
   (. canvas addEventListener "mouseup" (event-dispatcher ::mouse-up) false)
   (. canvas addEventListener "mouseout" (event-dispatcher ::mouse-up) false)
   
   {::shapefx/rebuild-shape-stream (:stream db)}))

(defn conj-in [map path val] (update-in map path #(-> % (vec) (conj val))))
(defn event-xy [event] 
  {:x (if (= 0 (.-offsetX event)) (.-layerX event) (.-offsetX event)) 
   :y (if (= 0 (.-offsetY event)) (.-layerY event) (.-offsetY event))})

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

(reg-event-fx
; mouse-down
 ::mouse-down
 (fn [{:keys [db]} [_ event]]  
   (-> event
       (event-xy)
       (update-stream-item (init-stream-item :free-hand (random-uuid)))
       (merge {:config (get-in db [:draw :config])})
       ((partial assoc db :active-stream))
       (#(assoc {} :db % ::shapefx/shape-modified (:active-stream %))))))


; if we have an active stream then update it
(reg-event-fx 
; mouse-move
 ::mouse-move
 (fn [{:keys [db]} [_ event]]
   (when-let [active-stream (:active-stream db)]
     (let [newDb (update db :active-stream (partial update-stream-item (event-xy event)))]
       {:db newDb
        ::shapefx/shape-modified (:active-stream newDb)}))))

(reg-event-fx 
; mouse-up
 ::mouse-up
 (fn [{:keys [db]} [_ _]]
   (when-let [stream-item (:active-stream db)]
     (let [item-id (:id stream-item)
           newDb (-> db
                     (assoc :active-stream nil)
                     (conj-in [:stream-order] item-id)
                     (assoc-in [:stream item-id] stream-item))]
       {:db newDb
        ::shapefx/shape-modified (get-in newDb [:stream item-id])}))))

(reg-event-fx
 ::undo
 (fn [{:keys [db]} [_ _]]
   (let [last-id (last (:stream-order db))
         new-db (->
                 db
                 (update :stream #(dissoc % last-id))
                 (update :stream-order butlast))]
     {:db new-db
      ::shapefx/rebuild-shape-stream (:stream new-db)})))

(reg-event-fx
 ::snapshot
 (fn [{:keys [db]}, [_ _]]
   (let [snap-id (v5 (prn-str (select-keys db [:stream :stream-order])) APP_UUID)]
     {:db (assoc-in
           db
           [:snapshots snap-id]
           (-> db 
               (select-keys [:stream :stream-order])
               (assoc :id snap-id 
                      :time (.local DateTime)
                      :preview (.toDataURL (.getElementById js/document "canvas") "image/png"))))})))

(reg-event-fx
 ::close-preview
 (fn [{:keys [db]} _]
   {:db (dissoc db :preview)
    ::shapefx/rebuild-shape-stream (:stream db)}))

(reg-event-fx
; clear-stream
 ::clear-stream
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db :stream {} :stream-order [] :snapshots {})
    ::shapefx/rebuild-shape-stream {}}))

(reg-event-fx
 ; preview snap
 ::preview-snap
 (fn [{:keys [db]} [_ snap-id]]
   {:db (assoc db :preview snap-id)
    ::shapefx/rebuild-shape-stream (get-in db [:snapshots snap-id :stream])}))