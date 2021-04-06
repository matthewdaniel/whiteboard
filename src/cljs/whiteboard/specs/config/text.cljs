(ns whiteboard.specs.config.text
  (:require [clojure.spec.alpha :as s]))

(s/def ::tool #{:text})
(s/def ::moving true?)
(s/def ::config (s/keys :req-un [::tool]
                        :opt-un [::moving]))

