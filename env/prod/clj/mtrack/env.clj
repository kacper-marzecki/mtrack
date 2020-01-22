(ns mtrack.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[mtrack started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[mtrack has shut down successfully]=-"))
   :middleware identity})
