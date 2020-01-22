(ns mtrack.todo.core
  (:use [clojure.java.io])

  (:require [clojure.core.async :as a]
            [mtrack.ws :refer [chsk-send!]]
            [clojure.java.io :as io]))

(def sample-task
  {:id          "name"
   :description ""
   :notes       [""]
   :time        0
   })

(defn write-todos [data] (spit "data.kek" (pr-str data)))
(defn read-todos []
  (if (.exists (io/file "data.kek"))
    (read-string (slurp "data.kek"))
    {})
  )

(def tasks (atom (read-todos)))
(defonce timed-tasks (atom {}))

(defn increment-time [tasks]
  (into {} (map (fn [x] [(first x) (inc (second x))]) (seq tasks))))

(defn get-task-by-id [id]
  (when-let [task (get @tasks id)]
    (merge {:id id} task)))

(defn start-timing [{:keys [id]}]
  (let [task (get-task-by-id id)]
    (swap! timed-tasks assoc id (:time task))))

(defn create-task [id]
  (let [existing (get-task-by-id id)]
    (if (nil? existing)
      (swap! tasks assoc id {:time 0 :description ""}))))

(defn stop-timing [{:keys [id]}]
  (swap! timed-tasks dissoc id))

(defonce file-sync
  (a/go-loop []
    (do
      (write-todos @tasks)
      (a/<! (a/timeout 5000))
      (recur))))

(def time-ticker
  (a/go-loop []
    (do
      (let [new-tasks (increment-time @timed-tasks)]
        (reset! timed-tasks new-tasks))
      (doall
        (map
          (fn [k] (swap! tasks update-in [k :time] (constantly (get @timed-tasks k))))
          (keys @timed-tasks)))
      (a/<! (a/timeout 1000))
      (recur))))

(defonce time-broadcaster
         (a/go-loop []
           (do
             ;(reset! timed-tasks (increment-time @timed-tasks))
             (chsk-send! :sente/all-users-without-uid [:server/tick {:tasks @timed-tasks}])
             (a/<! (a/timeout 1000))
             (recur))))

(defn get-task-list []
  (chsk-send! :sente/all-users-without-uid [:server/tasks (keys @tasks)]))

(defn update-task [task]
  (swap! tasks update-in [(:id task) :description] (constantly (:description task))))
;(defonce tasks-broadcaster
;         (a/go-loop []
;           (do
;             ;(reset! timed-tasks (increment-time @timed-tasks))
;             (chsk-send! :sente/all-users-without-uid [:server/tasks (keys @tasks)])
;             (a/<! (a/timeout 5000))
;             (recur))))
