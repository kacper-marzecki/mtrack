(ns mtrack.api
  (:require [mtrack.todo.core :as core]
            [mtrack.ws :as ws]
            [taoensso.sente :as sente]))

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id ; Dispatch on event-id
          )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
            ws/ch-chsk event-msg-handler)))
(start-router!)



;; IMPLEMENTATIONS OF EVENT HANDLERS
(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    ;(print (str "Unhandled event: %s" event))
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :mtrack/kek
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn  client-id]}]
  (clojure.pprint/pprint ev-msg)
  (send-fn :sente/all-users-without-uid [:routes/reply {:kek "one"}]))

(defmethod -event-msg-handler :mtrack/create-task
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn  client-id]}]
  (core/create-task ?data))

(defmethod -event-msg-handler :mtrack/start-timing
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn  client-id]}]
  (clojure.pprint/pprint (:?data ev-msg))
  (core/start-timing (:?data ev-msg))
  )
(defmethod -event-msg-handler :mtrack/stop-timing
  [{:as ev-msg :keys [?data]}]
  (clojure.pprint/pprint (:?data ev-msg))
  (core/stop-timing ?data)
  )
(defmethod -event-msg-handler :mtrack/get-task-list
  [_]
  (core/get-task-list))

