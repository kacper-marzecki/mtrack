(ns mtrack.db)

;  sample-todo
;  {:id "name"
;   :description ""
;   :time 0
;   :timed false
;   }

(def default-db
  {
   :tasks []
   :timers {}
   :new-task-input ""
   })

