(ns mtrack.core
  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)])
  (:require
    [day8.re-frame.http-fx]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [mtrack.ajax :as ajax]
    [mtrack.events]
    [reitit.core :as reitit]
    [clojure.string :as string]
    [cljs.core.async :as async :refer (<! >! put! chan)]
    [taoensso.sente :as sente :refer (cb-success?)]
    [mtrack.remote.api :refer [send-event!]]
    [re-frame.core :as re-frame])
  (:import goog.History))




(defn nav-link [uri title page]
  [:a.item
   {:href  uri
    :class (when (= page @(rf/subscribe [:page])) :is-active)}
   title])

(defn navbar []
  [:div.ui.inverted.menu
   [:div.ui.container
    [:div.header.item "mTrack"]
    [nav-link "#/" "Todo" :home]
    [nav-link "#/about" "About" :about]
    ]])

(defn about-page []
  [:div.ui.center.aligned.segment
   [:img {:src "/img/warning_clojure.png"}]])

(defn format-time
  [time]
  (let [h (int (divide time 3600))
        h-rest (mod time 3600)
        m (int (divide h-rest 60))
        s (- h-rest (* 60 m))]
    (str (if (= 0 h) "" (str h "h "))
         (if (= 0 m) "" (str m "m "))
         (if (= 0 s) "" (str s "s ")))))

(defn timer-component [task-id]
  (let [time (rf/subscribe [:time task-id])]
    (fn []
      [:<>
       (format-time @time)
       [:div.ui.small.buttons
        [:button.ui.button {:on-click (fn [] (send-event! [:mtrack/start-timing {:id task-id}]))}
         [:i.play.icon]]
        [:button.ui.button {:on-click (fn [] (send-event! [:mtrack/stop-timing {:id task-id}]))}
         [:i.pause.icon]]
        [:button.ui.button {:on-click (fn [] (send-event! [:mtrack/delete {:id task-id}]))}
         [:i.trash.icon]]]])))

(defn task-component
  [task-id]
  (let [task (rf/subscribe [:task task-id])]
    [:<>
     [:div.ui.grid.title.segment
      [:div.ten.wide.column {:font-size 10} [:i.dropdown.icon]  task-id]
      [:div.six.wide.column {:align "right"} [timer-component task-id]]]
     [:div.content
      [:div.ui.form
       [:div.field
        [:label "Description"]
        [:textarea {:on-change
                    (fn [event]
                      (send-event! [:mtrack/update-description {:id task-id
                                                                :description (-> event .-target .-value)}]))
                    :default-value (:description @task)}]]]]]))

(defn new-task-input
  []
  (let [input (rf/subscribe [:new-task-input])]
    (fn []
      [:div
       [:input {
                :on-change #(rf/dispatch [:new-task-input (-> % .-target .-value)])
                }]
       [:button.ui.button
        {:onClick (fn
                    []
                    (send-event! [:mtrack/create-task @input]))} "new "]])))

(defn home-page []
  (let [tasks (rf/subscribe [:tasks])]
    (fn []
      (do
        [:div.ui.container
        [:div.ui.center.aligned.segment [ new-task-input]]
        [:div.ui.accordion (for [[task-id body] (seq @tasks)]
                             ^{:key task-id} [task-component task-id])]]))))

(def pages
  {:home  #'home-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(let [p @(rf/subscribe [:page])]
      (p pages))]])

(def router
  (reitit/router
    [["/" :home]
     ["/about" :about]]))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (let [uri (or (not-empty (string/replace (.-token event) #"^.*#" "")) "/")]
          (rf/dispatch
            [:navigate (reitit/match-by-path router uri)]))))
    (.setEnabled true)))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [::mtrack.events/initialize-db])
  (rf/dispatch-sync [:navigate (reitit/match-by-name router :home)])
  (ajax/load-interceptors!)
  (hook-browser-navigation!)
  (mount-components)
  (send-event! [:mtrack/get-task-list])
  )
