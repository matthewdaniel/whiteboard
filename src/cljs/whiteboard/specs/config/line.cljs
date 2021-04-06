(ns whiteboard.specs.config.line
  (:require [clojure.spec.alpha :as s]))

(s/def ::arrow-type #{:end :start :both})

(s/def ::tool #{:line})
(s/def ::dashed true?)
(s/def ::config (s/keys :req-un [::tool]
                             :opt-un [::dashed ::arrow-type]))
