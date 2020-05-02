(ns whiteboard.subs
  (:require
   [re-frame.core :as rf]))
(rf/reg-sub
 ::snapshots
 #(:snapshots %))