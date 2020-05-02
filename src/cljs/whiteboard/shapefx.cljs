(ns whiteboard.shapefx
  (:require
   [re-frame.core :as rf]))

(defn connect-points [ctx [{x1 :x y1 :y} {x2 :x y2 :y}]]
  (when (and ctx (not= [x1 y1] [x2 y2]) x2)
    (.moveTo ctx x1 y1)
    (.lineTo ctx x2 y2)
    (.stroke ctx)))

(defn free-hand-updater [ctx {:keys [points]}]
  (when (= 2 (count (take-last 2 points)))
    (.beginPath ctx)
    (set! (.-strokeStyle ctx) "black")
    (set! (.-lineWidth ctx) 1)
    (connect-points ctx (take-last 2 points)))
  )

(rf/reg-fx
 ::shape-modified
 (fn [shape]
   (let [canvas (.getElementById js/document "canvas")
         ctx (.getContext canvas "2d")]
     (case (:type shape)
       :free-hand (free-hand-updater ctx shape)))))

(defn free-hand-redraw [ctx {:keys [points] :as all}]
  (when (= 2 (count (take-last 2 points)))
    (->> points 
         (partition 2 1) 
         (map #(assoc all :points %))
         (map (partial free-hand-updater ctx))
         (doall))))

(defn redraw-shape [ctx shape]
  (case (:type shape)
    :free-hand (free-hand-redraw ctx shape)))


(rf/reg-fx
 ::rebuild-shape-stream
 (fn [stream]
   (let [canvas (.getElementById js/document "canvas")
         ctx (.getContext canvas "2d")]
     (.clearRect ctx 0 0 (.-width canvas) (.-height canvas))
     (.restore ctx)
     ; redraw each shape
     (doall (map #(->> % (last) (redraw-shape ctx)) stream))
     )))

