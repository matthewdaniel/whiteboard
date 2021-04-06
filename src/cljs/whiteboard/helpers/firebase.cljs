(ns whiteboard.helpers.firebase
  (:require
   [clojure.string :refer [join]]
   ["@firebase/app" :refer [firebase]]))

(defonce firebase-app-info
  {:apiKey "AIzaSyCUQD8RHrgBTCMrWsk98lCw5Qorh3s9IDo"
   :authDomain "whiteboard-1a225.firebaseapp.com"
   :projectId "whiteboard-1a225"
   :storageBucket "whiteboard-1a225.appspot.com"
   :messagingSenderId "200780278960"
   :databaseURL "https://whiteboard-1a225-default-rtdb.firebaseio.com"
   :appId "1:200780278960:web:993d70526e8cb39f181f75"
   :measurementId "G-R7LB6DCGTP"})

;; (defonce inited (.initializeApp firebase (clj->js firebase-app-info)))
;; (defn db [] (.database firebase))
;; (defn store [] (.firestore firebase))

;; (def cljs-path->str #(->> % (map name) (join "/")))

;; (defn ref [path] (->> path (cljs-path->str) (.ref (db))))

(defn init [])

;; (defn k [] (.-key (.push (.ref (db)))))

;; (defn on [path callback]
;;   (.on (ref path) "value" #(callback (.val %))))