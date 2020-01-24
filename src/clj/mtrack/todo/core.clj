(ns mtrack.todo.core
  (:use [clojure.java.io])

  (:require [clojure.core.async :as a]
            [mtrack.ws :refer [chsk-send!]]
            [clojure.java.io :as io]))



(defn write-todos [data] (spit "data.kek" (pr-str data)))
(defn read-todos []
  (if (.exists (io/file "data.kek"))
    (read-string (slurp "data.kek"))
    {}))


;  sample-todo
;  {:id "name"
;   :description ""
;   :time 0
;   :timed false
;   }
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
             (chsk-send! :sente/all-users-without-uid [:server/tick {:tasks @tasks}])
             (a/<! (a/timeout 1000))
             (recur))))

(defn get-task-list []
  (chsk-send! :sente/all-users-without-uid [:server/tasks @tasks]))

(defn delete [{:keys [id]}]
  (swap! tasks dissoc :tasks id))

(defn update-description [{:keys [id description]}]
  (swap! tasks
         update-in [id :description]  (constantly description)))