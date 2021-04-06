(ns whiteboard.views
  (:require
   [luxon :refer [DateTime]]
   ["@fortawesome/fontawesome-free" :as fa]
   [whiteboard.helpers.shared :refer [<sub >evt >value-evt >kewordize-value-evt date-time-short]]
   [whiteboard.helpers.const :refer [get-tool-fa]]
   [whiteboard.subs :as subs]
   [clojure.string :refer [includes? lower-case]]
   [whiteboard.events :as events]
   [whiteboard.tools.text.events :as txt-events]
   [reagent.core :refer [create-class dom-node atom]]))


(def live-fill-color "black")
(def live-fill-opacity 0.05)
(defn- snap [{:keys [time id preview]}]
  [:div.snapshot {:key id :class (when (= id (<sub [::subs/preview-mode])) "active") :on-click #(>evt [::events/preview-snap id])}
   [:img.snap-preview {:src preview}]
   [:i.fas.fa-history.restore-snap {:on-click #(>evt [::events/restore-snapshot])}]
   [:div.description (.toLocaleString time (.-DATETIME_SHORT_WITH_SECONDS DateTime))]])

(defn color-button [color]
  [:button.color
   {:key color
    :style {:color color :border-color color :background-color color}
    :on-click #(>evt [::events/change-color color])}])

(defn arrow-select []
  [:select
   {:value (<sub [::subs/arrow-type])
    :on-change #(>kewordize-value-evt [::events/change-arrow-type %])}
   [:option {:value :none}  "None"]
   [:option {:value :start} "Start"]
   [:option {:value :end} "End"]
   [:option {:value :both} "Both"]])

;; (defonce active-action-menu (atom false))

(defn action-menu []
  (let [id (random-uuid)]
    (fn [tag fa title styles child]
      (let [vis (<sub [::subs/visible-menu])]
      [:div.action-menu
       {:style styles
        :class tag
        :on-click #(>evt [::events/set-visible-menu (when-not (= id vis) id)])}
       [:div.button
        [:div.ico {:class fa :title title}]]

       [:div.action-panel-container
        [:div.action-panel
         {:class (when (= vis id) "active")}
         [:div.child child]
         [:div.title title]]]])))
        )

(def is-mac (-> js/navigator (.-platform) (includes? "Mac")))
(def ctrl-key (if is-mac "Cmd" "Ctrl"))

(defn left-panel []
  [:div.left-panel
   [action-menu :tool (<sub [::subs/draw-tool-fa]) "Draw Tools" {}
    [:div.tools
     [:div {:class (get-tool-fa :free) :on-click #(>evt [::events/change-draw-tool :free])}]
     [:div {:class (get-tool-fa :text) :on-click #(>evt [::events/change-draw-tool :text])}]
     [:div {:class (get-tool-fa :line) :on-click #(>evt [::events/change-draw-tool :line])}]
     [:div {:class (get-tool-fa :poly) :on-click #(>evt [::events/change-draw-tool :poly])}]
     [:div {:class (get-tool-fa :oval) :on-click #(>evt [::events/change-draw-tool :oval])}]]]
  ;;  (when (<sub [::subs/shape-supports-arrows])
  ;;    [action-menu (<sub [::subs/draw-arrow-fa]) "Arrow Type"]
  ;;    [:div.action-item
  ;;     {:on-click #(>evt [::events/change-arrow-type %])}
  ;;     [:div.ico.fad.fa-arrows-h]
  ;;     [:div.desc [:span "Clear Canvas"]]])
   [action-menu :color "fas fa-palette" "Colors" {:color (<sub [::subs/draw-color])}
    [:div
     [:div.color-buttons
      (map color-button '("#f7412d" "#47b04b" "#1194f6" "#ffc200" "#9d1bb2"))]]]
   [action-menu :action "fas fa-ellipsis-h" "Actions" {}
    [:div.misc-actions
     [:div.action-item
      {:on-click #(>evt [::events/undo])}
      [:div.ico.fas.fa-undo]
      [:div.desc  [:span "Undo"] [:span.key-combo (str ctrl-key " + Z")]]]
    ;;  [:div.action-item
    ;;   :on-click (>evt [::events/undo])
    ;;   [:div.ico.fas.fa-redo]
    ;;   [:div.desc [:span "Redo"] [:span.key-combo (str ctrl-key " + Shift + Z")]]]
     [:div.action-item
      {:on-click #(>evt [::events/snapshot])}
      [:div.ico.fas.fa-camera]
      [:div.desc [:span "Snapshot"]]]
    ;;  [:div.action-item
    ;;   [:div.ico.fas.fa-chalkboard]
    ;;   [:div.desc [:span "Preview"]]]

     [:div.action-item
      {:on-click #(>evt [::events/clear-stream])}
      [:div.ico.fas.fa-times]
      [:div.desc [:span "Clear Canvas"]]]
     [:div.action-item
      {:on-click #(>evt [::events/reset-all])}
      [:div.ico.far.fa-trash-alt]
      [:div.desc [:span "Reset Project"]]]]]
   [action-menu :share "fas fa-link" "Share" {}
    [:div "some-link"]]
  
   ])

(defn preview-overlay []
  [:div.preview {:class (when-not (<sub [::subs/preview-mode]) "hidden")}
   [:div.preview-message
    [:div.preview-text "Preview Mode"]
    [:div.preview-close {:on-click #(>evt [::events/close-preview])}
     [:i.far.fa-times-circle]]]])

(defn marker [color id]
  [:marker
   {:id id
    :view-box "0 0 10 10"
    :refX 5
    :refY 5
    :marker-width 2.5
    :marker-height 2.5
    :orient "auto-start-reverse"}
   [:path {:d "M 0 0 L 10 5 L 0 10 z"
           :fill color}]])


(defn line-shape [stream-id is-live]
  (let [{:keys [points]} (<sub [::subs/stream-item stream-id])
        arrow-id (str "arrow-" stream-id)
        stream (<sub [::subs/stream-item stream-id])
        {:keys [line-color line-width arrow-type]} (:config stream)
        arrow-end (or (= :end arrow-type) (= :both arrow-type))
        arrow-start (or (= :start arrow-type) (= :both arrow-type))
        [{x1 :x y1 :y} {x2 :x y2 :y}] points]
    [:<>
     (when arrow-type [marker line-color arrow-id])
     [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2
             :marker-end (when arrow-end (str "url(#" arrow-id ")"))
             :marker-start (when arrow-start (str "url(#" arrow-id ")"))
             :key (str x1 ":" x2 ":" y1 ":" y2)
             :style {:stroke line-color
                     :stroke-width line-width
                     :stroke-dasharray (when is-live 4)}}]]))


(defn free-shape [stream-id is-live]
  (let [points (<sub [::subs/free-smoothed-line stream-id])
        stream (<sub [::subs/stream-item stream-id])
        arrow-id (str "arrow-" stream-id)
        {:keys [line-color line-width arrow-type]} (:config stream)
        arrow-end (or (= :end arrow-type) (= :both arrow-type))
        arrow-start (or (= :start arrow-type) (= :both arrow-type))]
    ;; (cljs.pprint/pprint [:here2 points])
    [:<>
     (when arrow-type [marker line-color arrow-id])
     [:path.stream.feee {:d points
                         :marker-end (when arrow-end (str "url(#" arrow-id ")"))
                         :marker-start (when arrow-start (str "url(#" arrow-id ")"))
                         :class (when is-live "live")
                         :stroke line-color
                         :stroke-width line-width
                         :stroke-linecap "round"
                         :stroke-linejoin "round"
                         :stroke-dasharray (when is-live 8)
                         :fill "none"}]]))


(defn poly-shape [stream-id is-live]
  (let [{:keys [points]} (<sub [::subs/stream-item stream-id])
        [{x1 :x y1 :y} {x2 :x y2 :y}] points
        stream (<sub [::subs/stream-item stream-id])
        {:keys [line-color line-width arrow-type]} (:config stream)]

    [:rect {:fill-opacity (if is-live live-fill-opacity 1)
            :fill (if is-live live-fill-color "transparent")
            :stroke line-color
            :stroke-width line-width
            :stroke-dasharray (when is-live 4)
            :x (min x1 x2)
            :y (min y1 y2)
            :width (js/Math.abs (- x2 x1))
            :height (js/Math.abs (- y2 y1))}]))

(defn oval-shape [stream-id is-live]
  (let [{:keys [points]} (<sub [::subs/stream-item stream-id])
        stream (<sub [::subs/stream-item stream-id])
        {:keys [line-color line-width]} (:config stream)
        [{x1 :x y1 :y} {x2 :x y2 :y}] points
        x-len (js/Math.abs (- x2 x1))
        y-len (js/Math.abs (- y2 y1))]
    [:ellipse {:fill-opacity (if is-live live-fill-opacity 1)
               :fill (if is-live live-fill-color "none")
               :stroke line-color
               :stroke-width line-width
               :stroke-dasharray (when is-live 4)

               :cx (+ x1 (/ (- x2 x1) 2))
               :cy (+ y1 (/ (- y2 y1) 2))
               :rx (/ x-len 2)
               :ry (/ y-len 2)}]))

(defn text-shape []
  (create-class
   {:reagent-render
    (fn []
      (let [{:keys [points text] :as all} (<sub [::subs/active-stream-id])
            [{x1 :x y1 :y}] points]
        [:input#text-shape
         {:auto-focus true
          :value text
          :auto-complete "off"
          :style {:left (str x1 "px") :top (str y1 "px")}
          :on-change #(>value-evt [::txt-events/text-change %])
                              ;;  :on-blur #(>evt [::txt-events/input-blur])
          }]))}))

(defn div-shape []
  (let [shape (<sub [::subs/shape])]
    (case shape
      :text [text-shape]
      nil)))

(defn shape [stream-id is-live]
  (let [shape (<sub [::subs/stream-item stream-id])]

    (case (-> shape :config :tool)
      :line [:<> {:key (str stream-id)} [line-shape stream-id is-live]]
      :poly [:<> {:key (str stream-id)} [poly-shape stream-id is-live]]
      :oval [:<> {:key (str stream-id)} [oval-shape stream-id is-live]]
      :free [:<> {:key (str stream-id)} [free-shape stream-id is-live]]
      ;; :text [text-shape]
      nil)))


(defn actual-svg []
  (let [stream-ids (<sub [::subs/non-active-stream-ids])]
    [:svg.svg-canvas
     {:preserveAspectRatio "xMidYMid meet"
      :id "user-canvas"}
     (doall (map shape stream-ids))]))

(defn svg-live []
  (let [active-stream-id (<sub [::subs/active-stream-id])]
    [:svg.svg-canvas
     {:preserveAspectRatio "xMidYMid meet"}
     [shape active-stream-id true]]))

(defn svg-live-canvas []
  (create-class
   {:component-did-mount #(->> % (dom-node) (conj [::events/initialize-canvas]) (>evt))
    :reagent-render (fn []
                      [:div.svg-canvas-wrapper
                       [svg-live]
                       [:div.canvas-overlay]])}))
(defn welcome []
  [:div.welcome
   "Draw Here"])

(def snap-expanded (atom false))

(defn snap-tile [snap]
  (js/console.log (:time snap))
  [:div.snapshot
   {:key (:id snap)}
   [:img.snap-img {:src (:preview snap)}]
   [:div.time (-> snap (:time) (date-time-short))]
   ])

(def last-snap-time (atom 0))
(def snap-animation-visible (atom false))

(defn snap-animation []
  ;; todo 
  (let [t (<sub [::subs/newest-snapshot-time])]
    (when (< @last-snap-time t)
      (reset! snap-animation-visible (< @last-snap-time t)))
    (reset! last-snap-time t)
    (when @snap-animation-visible
      (js/setTimeout #(reset! snap-animation-visible false)) 300)
    [:div.snap-animation
     {:class (if @snap-animation-visible "start" "finish")}
     [:span.fas.fa-camera-retro]]))

(defn snap-shot-tray []
  (let [snaps (<sub [::subs/snapshots])]

    [:<>
     (if (empty? snaps) nil
         [:div.snaps
          [:div.tray-header
           {:on-click #(swap! snap-expanded not)}
           [snap-animation]
           [:div.title "Snapshots"]
           [:div.expand [:span.ico.fas.fa-minus]]]
          (when @snap-expanded [:div.snapshot-panel
                                (map snap-tile snaps)])])]))

(defn main-panel []
  [:div#main
  ;;  (on [:test] #(cljs.pprint/pprint %))
   (when (<sub [::subs/welcome-visible]) [welcome])
   [left-panel]
   [:div#canvas-container
    ;; [div-shape]
    [actual-svg]
    [svg-live-canvas]
    [preview-overlay]]
   [snap-shot-tray]])