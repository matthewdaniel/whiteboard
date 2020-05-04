(ns whiteboard.subs
  (:require
   [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 ::full-snapshots
 #(:snapshots %))

(def snap-pair-to-minimal #(-> % last (select-keys [:time :id :name :preview])))

(def get-time-prop-as-string #(-> % (:time) (.toString)))

(reg-sub
 ::snapshots
 (fn [_ _] (subscribe [::full-snapshots]))
 (fn [snapshots]
   (->> snapshots (map snap-pair-to-minimal) (sort-by get-time-prop-as-string))))

(reg-sub
 ::draw-config
 #(get-in % [:draw :config]))

(reg-sub
 ::draw-line-width
 (fn [_ _] (subscribe [::draw-config]))
 #(:line-width %))

(reg-sub
 ::preview-mode
 (fn [{:keys [preview]}]
   preview))