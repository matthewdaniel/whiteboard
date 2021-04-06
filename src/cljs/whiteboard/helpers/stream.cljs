(ns whiteboard.helpers.stream
  (:require
   [whiteboard.helpers.fraction :as frac]
   [whiteboard.helpers.shared :refer [not-nan? nan?]]))

(defn event-xy [event]
  ; (js/console.log event)
  {:x (if (= 0 (.-offsetX event)) (.-layerX event) (.-offsetX event))
   :y (if (= 0 (.-offsetY event)) (.-layerY event) (.-offsetY event))})

(defn init-stream-item [id]
  {:id id :points []})


(defn point-distance [[{x1 :x y1 :y} {x2 :x y2 :y}]]
  (js/Math.sqrt (+ (js/Math.pow (- x2 x1) 2)  (js/Math.pow (- y2 y1) 2))))

(defn more-y? [[{x1 :x y1 :y} {x2 :x y2 :y}]]
  (> (js/Math.abs (- y2 y1)) (js/Math.abs (- x2 x1))))

(defn more-x? [[{x1 :x y1 :y} {x2 :x y2 :y}]]
  (> (js/Math.abs (- x2 x1)) (js/Math.abs (- y2 y1))))

(defn direct-right? [[{x1 :x} {x2 :x} :as points]]
  (and (more-x? points) (> x2 x1)))

(defn direct-left? [[{x1 :x} {x2 :x} :as points]]
  (and (more-x? points) (<= x2 x1)))

(defn direct-up? [[{y1 :y} {y2 :y} :as points]]
  (and (more-y? points) (> y2 y1)))

(defn direct-down? [[{y1 :y} {y2 :y} :as points]]
  (and (more-y? points) (<= y2 y1)))

(defn move-x [[_ {x2 :x} :as points] l]
  (cond
    (direct-left? points) (- x2 l)
    (direct-right? points) (+ x2 l)
    :else x2))

(defn move-y [[_ {y2 :y} :as points] l]
  (cond
    (direct-down? points) (- y2 l)
    (direct-up? points) (+ y2 l)
    :else y2))

(defn invert-point [{:keys [x y]}] {:x y :y x})
(def invert-points #(-> % (map invert-points)))

(defn point-back-by-plane [l2 points]
  (let [;; move back on line by l
        x (:x (last points))
        y (:y (last points))
        x1 (move-x points l2)
        y1 (move-y points l2)
        l (/ l2 2)]
    [{:x (if (more-x? points) x (- x l))
      :y (if (more-y? points) y (- y l))}
     {:x (if (more-x? points) x (+ x l))
      :y (if (more-y? points) y (+ y l))}
     {:x x1 :y y1}]))


(defn point-back-by-sloped [l2 [{x1 :x y1 :y} {x2 :x y2 :y}]]
  (let [rise (- y2 y1)
        run (- x2 x1)
        slope (/ rise run)
        slope-sqr (* slope slope)
        inverse-slope (* -1 (/ run rise))
        inverse-slope-sqr (* inverse-slope inverse-slope)

        ; get point on line
        l (if (< x1 x2) (* -1 l2) l2)
        x3 (- x2 (* l (js/Math.sqrt (/ 1 (+ 1 slope-sqr)))))
        y3 (- y2 (* (* l slope) (js/Math.sqrt (/ 1 (+ 1 slope-sqr)))))

        ; get two points 90 from point on line to draw the arrow
        new-x1 (+ x2 (* (/ l 2) (js/Math.sqrt (/ 1 (+ 1 inverse-slope-sqr)))))
        new-y1 (+ y2 (* (* (/ l 2) inverse-slope) (js/Math.sqrt (/ 1 (+ 1 inverse-slope-sqr)))))
        new-x2 (+ x2 (* (/ (* -1 l) 2) (js/Math.sqrt (/ 1 (+ 1 inverse-slope-sqr)))))
        new-y2 (+ y2 (* (* (/ (* -1 l) 2) inverse-slope) (js/Math.sqrt (/ 1 (+ 1 inverse-slope-sqr)))))]
    [{:x new-x1 :y new-y1} {:x new-x2 :y new-y2} {:x x3 :y y3}]))

(defn point-back-by [l [{x1 :x y1 :y} {x2 :x y2 :y} :as points]]
  (let [rise (- y2 y1)
        run (- x2 x1)
        slope (/ rise run)
        try-res (point-back-by-sloped l points)
        is-numbers (->> try-res (map vals) (flatten) (every? not-nan?))]
    (if is-numbers try-res (point-back-by-plane l points))))


(defn draw-arrow [ctx l [point1 point2]]
  (let [[wing-1 wing-2 tip] (point-back-by l [point1 point2])]

    (.beginPath ctx)
    (set! (.-lineWidth ctx) 1)
    (.moveTo ctx (:x tip) (:y tip))
    
    (.lineTo ctx (:x wing-1) (:y wing-1))
    (.lineTo ctx (:x wing-2) (:y wing-2))
    (.lineTo ctx (:x tip) (:y tip))
    (.stroke ctx)
    (.fill ctx)))