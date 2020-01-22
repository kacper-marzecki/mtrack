(ns mtrack.routes.home
  (:require
   [mtrack.layout :as layout]
   [clojure.java.io :as io]
   [mtrack.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit      :refer (get-sch-adapter)]
   [ring.middleware.keyword-params :as kp ]
   [ring.middleware.params :as params]
   [mtrack.ws :as ws]
   ))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 kp/wrap-keyword-params
                 params/wrap-params
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/chsk"
    {:get ws/ring-ajax-get-or-ws-handshake
     :post ws/ring-ajax-post}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

