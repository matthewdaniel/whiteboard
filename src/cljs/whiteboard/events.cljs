(ns whiteboard.events
  (:require

   [whiteboard.helpers.localstore :as store]
   [whiteboard.helpers.shared :refer [event-dispatcher >evt db-assert fx-db-assert]]
   [ajax.edn :refer [edn-response-format]]
   [save-svg-as-png :refer [svgAsDataUri svgAsPngUri]]
   [whiteboard.tools.free.events]
   [whiteboard.tools.line.events]
   [whiteboard.tools.oval.events]
   [whiteboard.tools.text.events]
   [whiteboard.tools.poly.events]
   [clojure.spec.alpha :as s]
   [whiteboard.specs.general :as specs]
   [whiteboard.helpers.stream :refer [event-xy]]
   [whiteboard.helpers.const :refer [APP_UUID]]
   [luxon :refer [DateTime]]
   [uuid :refer [v5]]
   [day8.re-frame.http-fx]
   [clojure.string :as str]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
   [whiteboard.db :as db]))

(defn get-user-id []
  (let [user-id? (store/get-key :user-id)
        user-id (or user-id? (random-uuid))]
    (store/set-key :user-id user-id)
    user-id))

(def default-draw-config
  {:line-width 2
   :tool :free
   :line-color (if (str/includes? (.-className js/document.body) "dark-mode") "#fff" "#000")})

(reg-event-db
 ::internal-set-config
 (fn [db [_ config]]
   (assoc db :config config)))

(reg-event-db
 ::change-arrow-type
 (fn [db [_ new-arrow-type]]

   (-> db
       (#(if (= new-arrow-type :none)
           (update-in % [:draw :config] dissoc :arrow-type)
           (assoc-in % [:draw :config :arrow-type] new-arrow-type)))
       (db-assert))))

(reg-event-db
 ::change-draw-tool
 (fn [db [_ new-tool]]
   (-> db
       (assoc-in [:draw :config :tool] new-tool)
       (db-assert))))

(reg-event-db
 ::set-visible-menu
 (fn [db [_ item]] 
   (assoc db :visible-menu item)))

(reg-event-db
 ::change-draw-line-width
 (fn [db [_ new-width]]
   (-> db
       (assoc-in [:draw :config :line-width] (js/Number new-width))
       db-assert)))

(reg-event-db
 ::change-color
 (fn [db [_ color]]
   (-> db
       (assoc-in [:draw :config :line-color] color)
       db-assert)))

(reg-event-db
 ::initialize-db
 (fn [_ _]
   (let [db (assoc db/default-db
                   :user-id (get-user-id)
                   :next-item-id 1
                   :stream {}
                   :stream-order []
                   :snapshots {}
                   :dark-mode true
                   :draw {:config default-draw-config}
                   :active-stream nil)]
     (db-assert db))))

(defn get-parent-dims [canvas]
  (->
   canvas
   (.-parentElement)
   (#(into {} {:width (.-offsetWidth %) :height (.-offsetHeight %)}))))

(defn tool-ns-map [tool]
  (case tool
    :free (ns-publics 'whiteboard.tools.free.events)
    :line (ns-publics 'whiteboard.tools.line.events)
    :text (ns-publics 'whiteboard.tools.text.events)
    :oval (ns-publics 'whiteboard.tools.oval.events)
    :poly (ns-publics 'whiteboard.tools.poly.events)))

(defn passthrough [{:keys [db]} [_ event canvas-evt]]
  (let [handler ((tool-ns-map (get-in db [:draw :config :tool])) (symbol event))
        xy (event-xy canvas-evt)
        e1 (-> db (handler xy) (fx-db-assert))
        e2 (if-not (= event "mouse-down-fx") e1 (assoc e1 :dispatch [::set-visible-menu nil]))]
    e2))

(reg-event-fx ::pass-mouse-down passthrough)
(reg-event-fx ::pass-mouse-move passthrough)
(reg-event-fx ::pass-mouse-up passthrough)
(reg-event-fx ::pass-mouse-out passthrough)

(reg-event-fx
; clear-stream
 ::clear-stream
 (fn [{:keys [db]} [_ _]]
   {:db (-> db (assoc :stream {} :stream-order []) (dissoc :shadow-shape) (db-assert))}))

(reg-event-fx
 ; reset-all
 ::reset-all
 (fn [{:keys [db]} [_ _]]
   {:db (-> db (assoc :stream {} :stream-order [] :snapshots {}) (db-assert))}))

(reg-event-fx
 ::initialize-canvas
 (fn [{:keys [db]} [_ canvas]]
   (. canvas addEventListener "mousedown" (event-dispatcher ::pass-mouse-down "mouse-down-fx") false)
   (. canvas addEventListener "mousemove" (event-dispatcher ::pass-mouse-move "mouse-move-fx") false)
   (. canvas addEventListener "mouseup" (event-dispatcher ::pass-mouse-up "mouse-up-fx") false)
   (. canvas addEventListener "mouseout" (event-dispatcher ::pass-mouse-out "mouse-up-fx") false)))

(reg-event-fx
 ::bad-http-result
 (fn [& all] (js/console.log (clj->js [:failed-to-load]))))

(defn fake-user-input [{:keys [points config]}]
  (>evt [::internal-set-config config])

  (let [canvas-points (map #(js/eval (str "new Object({offsetX: " (get % :x) ", offsetY: " (get % :y) "})")) points)
        first-point (first canvas-points)
        last-point (last canvas-points)
        mid (butlast (rest canvas-points))]

    (>evt [::pass-mouse-down "mouse-down-fx" first-point])
    (doall (map #(>evt [::pass-mouse-move "mouse-move-fx" %]) mid))
    (>evt [::pass-mouse-up "mouse-up-fx" last-point])))

(reg-event-fx
 ::load-remote-stream
 (fn [{:keys [db]} [_ data]]
   (js/setTimeout (fn []
                    (>evt [::clear-stream])
                    (js/console.time "loading-time")
                    (->> data
                         :stream
                         (into [])
                         (map last)
                         (take-last 500)
                         (map fake-user-input)
                         (doall))
                    nil) 10)))

(reg-event-fx
 ::undo
 (fn [{:keys [db]} [_ _]]
   (let [last-id (last (:stream-order db))
         new-db (->
                 db
                 (update :stream #(dissoc % last-id))
                 (update :stream-order drop-last))]
     {:db (db-assert new-db)})))

(reg-event-fx
 ::snapshot-received
 (fn [{:keys [db]} [event snap-id data-uri]]
   {:db (assoc-in db [:snapshots snap-id] {:id snap-id :time (.local DateTime) :preview data-uri :preview-length (count data-uri)})}))

(reg-fx
 ::snapshot-svg
 (fn [snap-id]
   (-> "user-canvas"
       (js/document.getElementById)
       (svgAsPngUri (clj->js {:encoderOptions 1}))
       (.then (event-dispatcher ::snapshot-received snap-id)))))

(reg-event-fx
 ::snapshot
 (fn [{:keys [db]}, [_ _]]
   (let [snap-id (v5 (prn-str (select-keys db [:stream :stream-order])) APP_UUID)]
     {::snapshot-svg snap-id})))

(reg-event-fx
 ::close-preview
 (fn [{:keys [db]} _]
   {:db (-> db (dissoc :preview) (db-assert))}))



(reg-event-fx
 ; preview snap
 ::preview-snap
 (fn [{:keys [db]} [_ snap-id]]
   {:db (-> db (assoc :preview snap-id) (db-assert))}))