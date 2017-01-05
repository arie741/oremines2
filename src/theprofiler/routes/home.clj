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

;;admin template

(defn adminnav []
  (hc/html [:li {:class "dropdown" :id "adminnav"}
              [:a {:class "dropdown-toggle" :data-toggle "dropdown" :href "#"} "USERS" 
                [:b {:class "caret"}]]
              [:ul {:class "dropdown-menu"}
                [:li [:a {:href "/userslist"} "USERS LIST"]]
                [:li [:a {:href "/adduser"} "DAFTAR USER"]]]]))

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
  [:div#content] (html/append (apply profilepanels profiles))
  [:li#adminnav] (if (db/admin? (session/get :username))
                    (html/html-content (adminnav))))

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
  [& message]
  [:div#mes] (html/html-content (apply str message)))

(defsnippet adduser "../resources/public/adduser.html"
  [:div#adduser]
  [& message]
  [:div#mes] (html/html-content (apply str message)))

(defsnippet trultableh "../resources/public/userslist.html"
  [:tr#trultableh]
  [])

(defn hctrultable [nuser pw admin]
  (hc/html [:tr [:td nuser]
                [:td pw]
                [:td (if (= admin 1) "Admin" "User")]
                [:td [:a {:href (str "/edit/" nuser) :class "btn btn-sm btn-black"} "EDIT"]]]))

(defn trmap [dat]
  (apply str (map #(hctrultable (:username %) (:password %) (:admin %)) dat)))

(defsnippet userslist "../resources/public/userslist.html"
  [:div#userslist]
  []
  [:tr#trultable] (html/substitute "")
  [:table#ultable] (html/html-content (trmap (db/alluser)))
  [:table#ultable] (html/prepend (trultableh)))

(defsnippet edituser "../resources/public/edituser.html"
  [:div#edituser]
  [nuser & mes]
  [:form#form-adduser] (html/set-attr :action (str "/edituser-action/" nuser))
  [:a#deleteuser] (html/set-attr :href (str "/deleteuser/" nuser))
  [:input#nuser] (html/set-attr :value nuser)
  [:div#mes] (html/html-content (apply str mes)))

;;validate

(defn validate [page & adminpage]
  (if (session/get :username)
    (if (db/admin? (session/get :username))
      (if (empty? adminpage)
        page
        (first adminpage))
      page)
    (loginpage)))

;;routes

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
        (indexpage (addprofile (mes "Profile berhasil tersimpan!" "green")) '()))))
  (GET "/adduser" []
    (validate (indexpage (searchform) '()) (indexpage (adduser) '())))
  (POST "/adduser-action" [nuser pw upw padmin]
    (if (= upw pw)
      (if (empty? (db/login-f nuser))
        (do
          (db/adduser nuser pw (Integer/parseInt padmin))
          (indexpage (adduser (mes "User telah terdaftar!" "green")) '()))
        (indexpage (adduser (mes "User sudah terdaftar sebelumnya." "red")) '()))
      (indexpage (adduser (mes "Password tidak cocok." "red")) '())))

  (GET "/userslist" []
    (validate (indexpage (searchform) '()) (indexpage (userslist) '())))
  (GET "/edit/:nuser" [nuser]
    (validate (indexpage (searchform) '()) (indexpage (edituser nuser) '())))
  (POST "/edituser-action/:nuser" [nuser pw upw padmin]
    (if (and (empty? pw) (empty? upw))
      (do
        (db/update-by-user nuser (apply :password (db/login-f nuser)) (Integer/parseInt padmin))
        (validate (indexpage (searchform) '()) (indexpage (userslist) '())))
      (if (= pw upw)
          (do
            (db/update-by-user nuser pw (Integer/parseInt padmin))
            (validate (indexpage (searchform) '()) (indexpage (userslist) '())))
          (validate (indexpage (searchform) '()) 
                    (indexpage (edituser "arie" (mes "Password tidak sama!" "red")) '())))))
  (GET "/deleteuser/:nuser" [nuser]
    (if (db/admin? (session/get :username))
      (do
        (db/delete-by-user nuser)
        (validate (indexpage (searchform) '()) (indexpage (userslist) '())))
      (validate (indexpage (searchform) '()))))
  (GET "/logout" []
    (do 
      (session/clear!)
      (loginpage)))
  (route/not-found "Not Found"))