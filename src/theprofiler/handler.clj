(ns theprofiler.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [theprofiler.routes.home :refer :all]
            [noir.util.middleware :as middleware]
            [noir.session :as session]))

(def app (middleware/app-handler
       [app-routes]
       :ring-defaults (assoc-in site-defaults [:security :anti-forgery] false)))

;(def app
;  (wrap-defaults app-routes (assoc-in site-defaults [:security :anti-forgery] false)))
