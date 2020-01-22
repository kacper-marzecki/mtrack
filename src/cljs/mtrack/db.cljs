(ns mtrack.db)

(def sample-todo
  {:id "name"
   :description ""
   :notes [""]
   :time 0
   :timed false
   })

(def default-db
  {
   :tasks []
   :timers {}
   :new-task-input ""
   })

