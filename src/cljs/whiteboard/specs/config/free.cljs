(ns whiteboard.specs.config.free
  (:require [clojure.spec.alpha :as s]))

(s/def ::arrow-type #{:end :start :both})

(s/def ::tool #{:free})
(s/def ::config (s/keys :req-un [::tool]
                        :opt-un [::arrow-type]))
