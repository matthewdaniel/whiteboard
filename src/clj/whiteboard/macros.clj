(ns whiteboard.macros
  )

(defmacro db-event-defn [& args]
  (let [a :v]
    ;; (s/assert ::specs/app-db db)
    (cons 'defn args)))

(macroexpand 'db-event-defn)