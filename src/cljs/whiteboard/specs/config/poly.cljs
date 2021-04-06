(ns whiteboard.specs.config.poly
  (:require [clojure.spec.alpha :as s]))

(s/def ::tool #{:poly})
(s/def ::dashed true?)
(s/def ::side-count (s/and pos-int? #(> % 2)))
(s/def ::config (s/keys :req-un [::tool]
                        :opt-un [::dashed]))
