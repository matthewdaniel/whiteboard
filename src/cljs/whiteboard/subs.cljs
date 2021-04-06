(ns whiteboard.subs
  (:require
   [whiteboard.helpers.shared :refer [empty-str-if-nil]]
   [whiteboard.helpers.const :refer [get-tool-fa]]
   [clojure.string :refer [join]]
   [d3-shape :refer [line curveCatmullRom curveCardinal]]
   [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 ::full-snapshots
 #(:snapshots %))

(def snap-pair-to-minimal #(-> % last (select-keys [:time :id :name :preview])))

(def get-time-prop-as-string #(-> % (:time) (.toString)))

(reg-sub
 ::visible-menu
 #(:visible-menu %))

(reg-sub
 ::snapshots
 (fn [_ _] (subscribe [::full-snapshots]))
 (fn [snapshots]
   (->> snapshots (map snap-pair-to-minimal) (sort-by get-time-prop-as-string))))

(reg-sub
 ::newest-snapshot-time
 (fn [_ _] (subscribe [::full-snapshots]))
 (fn [snaps] 
   (->> snaps (vals) (apply max-key #(:time %)) (:time))))

(reg-sub
 ::draw-config
 #(get-in % [:draw :config]))

(reg-sub
 ::draw-color
 #(get-in % [:draw :config :line-color]))

(reg-sub
 ::draw-line-width
 (fn [_ _] (subscribe [::draw-config]))
 #(:line-width %))

(reg-sub
 ::draw-tool
 (fn [_ _] (subscribe [::draw-config]))
 #(-> % :tool))

(reg-sub
 ::draw-tool-fa
 (fn [_ _ ] (subscribe [::draw-tool]))
 get-tool-fa)

(reg-sub
 ::draw-arrow-fa
 (fn [_ _] (subscribe [::arrow-type]))
 #(case %
    :none "fa"
    :start
    :end
    :both
    :else ))

(reg-sub
 ::preview-mode
 (fn [{:keys [preview]}]
   preview))

(reg-sub
 ::shadow-shape
 #(-> % :shadow-shape))

(reg-sub
 ::active-stream-id
 #(-> % :active-stream))

(reg-sub
 ::welcome-visible
 (fn [_ _] (subscribe [::stream]))
 #(< (count %) 1))

(reg-sub
 ::arrow-type
 (fn [_ _] (subscribe [::draw-config]))
 #(-> % :arrow-type (empty-str-if-nil)))

(reg-sub
 ::shape-supports-arrows
 (fn [_ _] (subscribe [::draw-tool]))
 (fn [tool] (contains? #{:line :free} tool)))

(reg-sub
 ::active-item-points
 (fn [_ _] (subscribe [::active-stream-id]))

 #(-> % :points))

(def line-maker (line))
(reg-sub
 ::free-smoothed-line
 (fn [[_ stream-id]] (subscribe [::stream-item stream-id]))
 (fn [{:keys [points]}]
   (->> points
        (map vals)
        (doall)
        (clj->js)
        ;; (fn [l] (js/console.log :here l) l)
        (line-maker))))

(reg-sub
 ::stream
 #(-> % :stream))

(reg-sub
 ::non-active-stream-ids
 (fn [_ _] [(subscribe [::stream]) (subscribe [::active-stream-id])])
 (fn [[stream active-stream-id]] (keys (dissoc stream active-stream-id)) ))

(reg-sub
 ::stream-item
 (fn [db [_ stream-id]]
   (get-in db [:stream stream-id])))