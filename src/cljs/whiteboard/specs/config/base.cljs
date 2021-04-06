(ns whiteboard.specs.config.base
  (:require [clojure.spec.alpha :as s]))

; configurations
(s/def ::line-width pos-int?)
(s/def ::color #(re-matches #"^#(?:[0-9a-fA-F]{3}){1,2}([0-9a-fA-F]{2})?$" %))
(s/def ::line-color ::color)

(s/def ::config (s/keys :req-un [::line-width ::line-color]))

