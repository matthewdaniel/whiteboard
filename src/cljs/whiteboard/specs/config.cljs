(ns whiteboard.specs.config
  (:require [clojure.spec.alpha :as s]
            [whiteboard.specs.config.base :as base]
            [whiteboard.specs.config.line :as line]
            [whiteboard.specs.config.poly :as poly]
            [whiteboard.specs.config.oval :as oval]
            [whiteboard.specs.config.text :as text]
            [whiteboard.specs.config.free :as free]))

(s/def
  ::config
  (s/merge
   ::base/config
   (s/or
    :line ::line/config
    :text ::text/config
    :free ::free/config
    :poly ::poly/config
    :oval ::oval/config
    )))