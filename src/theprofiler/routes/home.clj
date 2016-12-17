(ns theprofiler.routes.home
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [net.cgrand.enlive-html :refer [deftemplate defsnippet] :as html]
    [hiccup.core :as hc]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [theprofiler.db :as db]))

(deftemplate base "../resources/public/index.html"
                  []
                  )

(defsnippet login "../resources/public/login.html"
  [:div#headerwrap]
  [])

(defn mes [message color]
  (hc/html [:div {:class (str "panel-" color)} (apply str message)]))

(deftemplate loginpage "../resources/public/base.html"
  [& message]
  [:div#content] (html/content (login))
  [:div#message] (html/html-content (apply str message)))

(defroutes app-routes
  (GET "/af" request
    (str (:ring.middleware.anti-forgery/anti-forgery-token 
      (:session request))))
  (GET "/" request
       (apply str (loginpage)))
  (POST "/login-action" {params :params}
    (let [username (:username params)
          password (:password params)] 
      (if (= password (apply :password (db/login-f username)))
        (str "WELCOME " username)
        (loginpage (mes "Either you put the wrong password or your username does not exists." "red")))))
  (route/not-found "Not Found"))
