(ns theprofiler.routes.home
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [net.cgrand.enlive-html :refer [deftemplate defsnippet] :as html]
    [hiccup.core :as hc]
    [ring.util.anti-forgery :refer [anti-forgery-field]]
    [theprofiler.db :as db]
    [noir.session :as session]))

(defsnippet searchform "../resources/public/search.html"
  [:div#searchf]
  [& searchq]
  [:div.searchq] (html/html-content (apply str searchq)))

(defsnippet profilet "../resources/public/profilethumb.html"
  [:div.profile-panel]
  [image pname age id]
  [:span.profileimg] (html/html-content image)
  [:span.name] (html/html-content pname)
  [:span.age] (html/html-content age)
  [:a.plink] (html/set-attr :href (str "/profile/" id)))

(defn profilepanels [profiles]
  (map #(profilet (:photos %) (:name %) (:age %) (:uuid %)) profiles))

(defsnippet profilep "../resources/public/profile.html"
  [:div#profile]
  [nama umur alamat kasus job org id photos]
  [:li.pname] (html/append nama)
  [:li.pumur] (html/append (str umur))
  [:li.palamat] (html/append alamat)
  [:li.pkasus] (html/append kasus)
  [:li.pjob] (html/append job)
  [:li.porg] (html/append org)
  [:li.pid] (html/append (str id))
  [:li.pphotos] (html/append photos))

(deftemplate indexpage "../resources/public/index.html"
  [snippet & profiles]
  [:div#content] (html/content snippet)
  [:div#content] (html/append (apply profilepanels profiles)))

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
  (GET "/" request
       (if (session/get :username)
        (indexpage (searchform) '())
        (loginpage)))
  (POST "/login-action" {params :params}
    (let [username (:username params)
          password (:password params)] 
      (if (not= 0 (count (db/login-f username)))
        (if (= password (apply :password (db/login-f username)))
              (do 
                (session/put! :username username)
                (indexpage (searchform) '()))
              (loginpage (mes "Wrong password or the username doesn't exist" "red")))
        (loginpage (mes "Wrong password or the username doesn't exist" "red")))))
  (POST "/search-action" request
    (let [res (:searchf (:params request))]
      (indexpage (searchform (str "Search Results: " (count (db/searchf res)))) (db/searchf res))))
  (GET "/profile/:id" [id]
    (let [pnama (apply :name (db/searchid (str id)))
          pumur (apply :age (db/searchid (str id)))
          palamat (apply :address (db/searchid (str id)))
          pkasus (apply :case (db/searchid (str id)))
          pjob (apply :job (db/searchid (str id)))
          porg (apply :organisation (db/searchid (str id)))
          uuid (apply :uuid (db/searchid (str id)))
          pphotos (apply :photos (db/searchid (str id)))]
      (indexpage (profilep pnama pumur palamat pkasus pjob porg uuid pphotos) '())))
  (GET "/session" request
    (str (session/get :username)))
  (GET "/logout" []
    (do 
      (session/clear!)
      (loginpage)))
  (route/not-found "Not Found"))
