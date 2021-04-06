(ns whiteboard.helpers.shared
  (:require
   [clojure.spec.alpha :as s]
   [whiteboard.specs.general :as specs]
   [luxon :refer [DateTime]]
   [camel-snake-kebab.core :as csk]
   [re-frame.core :as rf]))

(def <sub (comp deref rf/subscribe))
(def >evt rf/dispatch)
(def >value-evt (fn [[evt js-event]]
                  (rf/dispatch [evt (-> js-event .-target .-value)])))
(defn nan? [x] (false? (== x x)))
(def not-nan? (complement nan?))

(def >kewordize-value-evt (fn [[evt js-event]]
                            (rf/dispatch [evt (-> js-event .-target .-value (csk/->kebab-case-keyword))])))

(defn conj-in [map path val] (update-in map path #(-> % (vec) (conj val))))

(defn event-dispatcher [& args] #(-> args (vec) (conj %) (rf/dispatch)))

(defn js-to-clj [obj]
  (->> obj
       (js-keys)
       (js->clj)
       (map #(conj [(csk/->kebab-case-keyword %)] (aget obj %)))
       (into {})))
(defn set-js-key [obj key value]
  (let [prop (->> key (name) (csk/->camelCase))]
    (aset obj prop value))
  obj)


(defn db-assert [db]
  ;; (when (and (not= nil db) (not (s/valid? ::specs/app-db db))) (s/explain ::specs/app-db db))
  db)

(defn fx-db-assert [{:keys [db] :as all}]
  ;; (when (and (not= nil db) (not (s/valid? ::specs/app-db db))) (s/explain ::specs/app-db db))
  all)


(defn empty-str-if-nil [to-test]
  (if (nil? to-test) "" to-test))

(defn date-time-short [date] (.toLocaleString date (.-DATETIME_SHORT DateTime)))

(defn get-search-param [url param] 
  (as-> url v (new js/URL v) (.-searchParams v) (.get v param)))

(defn get-href [] (-> js/window (.-location) (.-href)))