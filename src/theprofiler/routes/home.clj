(ns theprofiler.routes.home
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [net.cgrand.enlive-html :refer [deftemplate defsnippet] :as html]
    [hiccup.core :as hc]
    [theprofiler.db :as db]
    [noir.session :as session]
    [noir.io :as io]
    [noir.response :as resp]))

;;helper functions

(defn validate [page]
  (if (session/get :username)
    (if (db/admin? (session/get :username))
      page
      page)
    (loginpage)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn up-file [pth profilephoto] 
  (if (not (empty? (:filename profilephoto)))
    (io/upload-file pth profilephoto)
    nil))

(defn up-file-multiple [pth photos]
  (if (vector? photos)
    (doseq [i photos]
      (io/upload-file pth i))
    (if (not (empty? (:filename photos)))
      (io/upload-file pth photos))))

;;templates

(defsnippet searchform "../resources/public/search.html"
  [:div#searchf]
  [& searchq]
  [:div.searchq] (html/html-content (apply str searchq)))

(defsnippet profilet "../resources/public/profilethumb.html"
  [:div.profile-panel]
  [image pname age kasus id]
  [:img.profileimg] (html/set-attr :src image)
  [:span.name] (html/html-content pname)
  [:span.age] (html/html-content age)
  [:span.kasus] (html/html-content kasus)
  [:a.plink] (html/set-attr :href (str "/profile/" id)))

(defn profilepanels [profiles]
  (map #(profilet (:profilephoto %) (:name %) (:age %) (:kasus %) (:uuid %)) profiles))

(defn photosimage [vec]
  (apply str (map #(hc/html [:a {:href %}
                              [:img {:src % :width "200px" :height "200px" :class "profphotos"}]]) vec)))

(defsnippet profilep "../resources/public/profile.html"
  [:div#profile]
  [nama umur alamat kasus job org id photos pimage]
  [:li.pname] (html/append nama)
  [:li.pumur] (html/append (str umur))
  [:li.palamat] (html/append alamat)
  [:li.pkasus] (html/append kasus)
  [:li.pjob] (html/append job)
  [:li.porg] (html/append org)
  [:li.pid] (html/append (str id))
  [:div.pphotos] (html/html-content (photosimage photos))
  [:div.photost] (html/append (str "( " (count photos) " )"))
  [:img.pimage] (html/set-attr :src pimage))

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

(defsnippet addprofile "../resources/public/addprofile.html"
  [:div#addprofile]
  [])

(defroutes app-routes
  (GET "/" request
       (if (session/get :username)
        (validate (indexpage (searchform) '()))
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
          pkasus (apply :kasus (db/searchid (str id)))
          pjob (apply :job (db/searchid (str id)))
          porg (apply :organisation (db/searchid (str id)))
          uuid (apply :uuid (db/searchid (str id)))
          pphotos (read-string (apply :photos (db/searchid (str id))))
          pimage (apply :profilephoto (db/searchid (str id)))]
      (validate (indexpage (profilep pnama pumur palamat pkasus pjob porg uuid pphotos pimage) '()))))
  (GET "/addprofile" request
    (validate (indexpage (addprofile) '())))
  (POST "/addprofile-action" {params :params}
    (let [qid (uuid)
          pth (str "resources/public/profiles/" qid)
          photos (if (vector? (:photos params)) 
                    (str (mapv #(str "/profiles/" qid "/" (:filename %)) (:photos params)))
                    (if (empty? (:filename (:photos params)))
                      "[]"
                      (str (vector (str "/profiles/" qid "/" (:filename (:photos params)))))))
          pphoto (if (empty? (:filename (:profilephoto params)))
                    ""
                    (str "/profiles/" qid "/" (:filename (:profilephoto params))))]
      (do
        (io/create-path pth true)
        (up-file pth (:profilephoto params))
        (up-file-multiple pth (:photos params))
        (db/addprofdb (:name params) (Integer/parseInt (:age params)) (:address params) (:kasus params) (:job params) (:org params) photos pphoto qid)
        (indexpage (addprofile) '()))))

  (GET "/logout" []
    (do 
      (session/clear!)
      (loginpage)))
  (route/not-found "Not Found"))