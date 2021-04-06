(ns whiteboard.helpers.fraction
  (:require [fraction.js :refer [Fraction]]))

(defn init
  ([num] (new Fraction num))
  ([num num2] (new Fraction num num2)))
(defn to-string [frac] (.toString frac))

(defn add
  ([frac num] (.add frac num))
  ([frac num num2] (.add frac num num2)))

(defn sub
  ([frac num] (.sub frac num))
  ([frac num num2] (.sub frac num num2)))

(defn mul
  ([frac num] (.mul frac num))
  ([frac num num2] (.mul frac num num2)))

(defn div
  ([frac num] (.div frac num))
  ([frac num num2] (.div frac num num2)))

(defn round
  ([frac] (.round frac))
  ([frac num] (.round frac num)))

(defn inverse
  ([frac] (.inverse frac)))

(defn neg
  ([frac] (.neg frac)))


(defn pow
  [frac num] (.pow frac num))

(defn to-val
  [frac] (.valueOf frac))



(defn testing 
  ([] :one)
  ([test] :two))

(testing "red")

