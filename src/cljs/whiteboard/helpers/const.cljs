(ns whiteboard.helpers.const)

(def APP_UUID "fac318fe-8757-4632-8f23-4e424bbe7d9d")

(def get-tool-fa
  #(case %
     :free "fas fa-pen-fancy"
     :text "fas fa-paragraph"
     :line "fas fa-grip-lines"
     :poly "far fa-object-ungroup"
     :oval "far fa-circle"
     :else "fas fa-question"))

(def ice 
  {:iceServers [{:urls ["stun:stun1.l.google.com:19302", "stun:stun2.l.google.com:19302"]}]
   :iceCandidatePoolSize 10})