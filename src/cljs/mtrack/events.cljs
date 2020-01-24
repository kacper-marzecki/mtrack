(ns mtrack.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [mtrack.db :as db]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))


(rf/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
             db/default-db))

(rf/reg-event-db
  :navigate
  (fn [db [_ route]]
    (assoc db :route route)))

(rf/reg-event-db
  :server/tick
  (fn [db [_ tasks]]
    (assoc db :tasks (:tasks tasks) )))

(rf/reg-event-db
  :server/tasks
  (fn [db [_ tasks]]
    (assoc db :tasks tasks)))

(rf/reg-event-db
  :new-task-input
  (fn [db [_ input]]
    (assoc db :new-task-input input)))

(rf/reg-sub
  :tasks
  (fn [db _]
    (:tasks db)))

(rf/reg-sub
  :new-task-input
  #(:new-task-input %1))

(rf/reg-sub
  :time
  (fn [db [_ id]]
    (-> db :tasks (get id ) :time )))

(rf/reg-sub
  :route
  (fn [db _]
    (:route db )))

(rf/reg-sub
  :page
  :<- [:route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :task
  (fn [db [_ task-id]]
    (-> db :tasks (get task-id))))