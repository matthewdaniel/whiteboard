(ns whiteboard.specs.general
  (:require [clojure.spec.alpha :as s]
            [whiteboard.specs.config :as config]
            ;; [clojure.spec.test.alpha :as stest]
            ))

(s/def ::stream-id pos-int?)

(s/def ::next-item-id ::stream-id)
(s/def ::id ::stream-id)
(s/def ::uuid-str? #(-> % (uuid) uuid?))

(s/def ::user-id uuid?)



(s/def ::x number?)
(s/def ::y number?)

(s/def ::xy (s/keys :req-un [::x ::y]))
(s/def ::points (s/coll-of ::xy))

(s/def ::config ::config/config)
(s/def ::stream-item (s/keys :req-un [::id ::points ::config]))


(s/def ::stream (s/map-of ::stream-id (s/merge ::stream-item (s/keys :req-un [::id]))))
(s/def ::active-stream ::stream-id)
(s/def ::stream-order (s/coll-of ::stream-id))

(s/def ::snapshot (s/keys :req-un [::stream ::stream-order]))
(s/def ::snapshots (s/map-of ::uuid-str? ::snapshot))

(s/def ::app-db 
       (s/keys 
        :req-un 
        [::next-item-id ::user-id ::stream ::snapshots]
        :opt-un [::active-stream]))

