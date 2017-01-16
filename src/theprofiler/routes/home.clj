(ns theprofiler.routes.home
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [net.cgrand.enlive-html :refer [deftemplate defsnippet] :as html]
    [hiccup.core :as hc]
    [theprofiler.db :as db]
    [noir.session :as session]
    [noir.io :as io]
    [noir.response :as resp]
    [clojure.java.io :as cio]))

;;helper functions

(defn merge-photo [photos pphoto id]
  (let [nphotos (str (apply vector (distinct (conj (read-string photos) pphoto))))]
    (if (not (empty? pphoto))
      (db/update-by-id id {:photos nphotos}))))

(defn delete-folder [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (cio/delete-file f))]
    (func func (cio/file fname))))

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

(defn update-file-multiple [pth photos id]
  (let [upth (str "/profiles/" id "/")
        d-vec (read-string (:photos (first (db/searchid id))))]
    (if (vector? photos)
      (db/update-by-id id 
        {:photos (str (apply vector (distinct (concat 
                                                d-vec
                                                (mapv #(str upth (:filename %)) photos)))))})
      (if (not (empty? (:filename photos)))
        (db/update-by-id id {:photos (str (apply vector (distinct (conj 
                                                                    d-vec
                                                                    (str upth (:filename photos))))))})))))

(defn exist? [vc st]
  (loop [i vc]
    (if (empty? i)
      false
      (if (= (first i) st)
          true
          (recur (rest i))))))

(defn img-exists? [img]
  (if (not (empty? img))
    img
    "/assets/img/post03.jpg"))

;;admin template

(defn adminnav []
  (hc/html [:li {:class "dropdown" :id "adminnav"}
              [:a {:class "dropdown-toggle" :data-toggle "dropdown" :href "#"} "USERS" 
                [:b {:class "caret"}]]
              [:ul {:class "dropdown-menu"}
                [:li [:a {:href "/userslist"} "USERS LIST"]]
                [:li [:a {:href "/adduser"} "DAFTAR USER"]]]]))

;;templates

(defsnippet searchform "public/search.html"
  [:div#searchf]
  [& searchq]
  [:div.searchq] (html/html-content (apply str searchq)))

(defsnippet profilet "public/profilethumb.html"
  [:div.profile-panel]
  [image pname age kasus id]
  [:img.profileimg] (html/set-attr :src image)
  [:span.name] (html/html-content pname)
  [:span.age] (html/html-content age)
  [:span.kasus] (html/html-content kasus)
  [:a.p-delete-btn] (html/set-attr :href (str "/deleteprof/" id))
  [:button.usure-modal-btn] (html/set-attr :data-target (str "#modal-" id))
  [:div.modal-usure] (html/set-attr :id (str "modal-" id))
  [:a.p-edit-btn] (html/set-attr :href (str "/editprof/" id))
  [:a.plink] (html/set-attr :href (str "/profile/" id)))

(defn profilepanels [profiles]
  (map #(profilet (img-exists? (:profilephoto %)) (:name %) (:age %) (:kasus %) (:uuid %)) profiles))

(defn photosimage [vec]
  (apply str (map #(hc/html [:a {:href (str "/photodet" %)}
                              [:img {:src % :width "200px" :height "200px" :class "profphotos"}]]) vec)))

(defsnippet profilep "public/profile.html"
  [:div#profile]
  [nama umur alamat kasus job org id photos pimage]
  [:div.pname] (html/append nama)
  [:div.pumur] (html/append (str umur))
  [:div.palamat] (html/append alamat)
  [:div.pkasus] (html/append kasus)
  [:div.pjob] (html/append job)
  [:div.porg] (html/append org)
  [:div.pid] (html/append (str id))
  [:a.profimghref] (html/set-attr :href (str "/photodet" pimage))
  [:div.pphotos] (html/html-content (photosimage photos))
  [:div.photost] (html/append (str "( " (count photos) " )"))
  [:img.pimage] (html/set-attr :src (img-exists? pimage))
  [:form#form-addphoto] (html/set-attr :action (str "/addphoto-action/" id))
  [:a.p-delete-btn] (html/set-attr :href (str "/deleteprof/" id))
  [:button.usure-modal-btn] (html/set-attr :data-target (str "#modal-" id))
  [:div.modal-usure] (html/set-attr :id (str "modal-" id))
  [:a.p-edit-btn] (html/set-attr :href (str "/editprof/" id)))

(defsnippet footerp "public/footer.html"
  [:div#footerwrap]
  [])

(deftemplate indexpage "public/index.html"
  [snippet & profiles]
  [:div#content] (html/content snippet)
  [:div#content] (html/append (apply profilepanels profiles))
  [:li#adminnav] (if (db/admin? (session/get :username))
                    (html/html-content (adminnav)))
  [:div#footer] (html/content (footerp)))

(defsnippet login "public/login.html"
  [:div#headerwrap]
  [])

(defn mes [message color]
  (hc/html [:div {:class (str "panel-" color)} (apply str message)]))

(deftemplate loginpage "public/base.html"
  [& message]
  [:div#content] (html/content (login))
  [:div#message] (html/html-content (apply str message)))

(defsnippet addprofile "public/addprofile.html"
  [:div#addprofile]
  [& message]
  [:div#mes] (html/html-content (apply str message)))

(defsnippet adduser "public/adduser.html"
  [:div#adduser]
  [& message]
  [:div#mes] (html/html-content (apply str message)))

(defsnippet trultableh "public/userslist.html"
  [:tr#trultableh]
  [])

(defn hctrultable [nuser pw admin]
  (hc/html [:tr [:td nuser]
                [:td pw]
                [:td (if (= admin 741)
                      "Master"
                      (if (= admin 1) 
                        "Admin" 
                        "User"))]
                [:td [:a {:href (str "/edit/" nuser) :class "btn btn-sm btn-black"} "EDIT"]]]))

(defn trmap [dat]
  (apply str (map #(hctrultable (:username %) (:password %) (:admin %)) dat)))

(defsnippet userslist "public/userslist.html"
  [:div#userslist]
  []
  [:tr#trultable] (html/substitute "")
  [:table#ultable] (html/html-content (trmap (db/alluser)))
  [:table#ultable] (html/prepend (trultableh)))

(defsnippet edituser "public/edituser.html"
  [:div#edituser]
  [nuser & mes]
  [:form#form-adduser] (html/set-attr :action (str "/edituser-action/" nuser))
  [:a#deleteuser] (html/set-attr :href (str "/deleteuser/" nuser))
  [:input#nuser] (html/set-attr :value nuser)
  [:div#mes] (html/html-content (apply str mes)))

(defsnippet editsuperuser "public/editsuperuser.html"
  [:div#editsuperuser]
  [nuser & mes]
  [:form#form-adduser] (html/set-attr :action (str "/edituser-action/" nuser))
  [:input#nuser] (html/set-attr :value nuser)
  [:div#mes] (html/html-content (apply str mes)))

(defsnippet editprofile "public/editprofile.html"
  [:div#editprofile]
  [profpic pname age address kasus job org photos id]
  [:img.pimage] (html/set-attr :src profpic)
  [:a.profimghref] (html/set-attr :href (str "/photodet" profpic))
  [:input#name] (html/set-attr :value pname)
  [:input#age] (html/set-attr :value age)
  [:textarea#address] (html/content (str address))
  [:input#kasus] (html/set-attr :value kasus)
  [:input#job] (html/set-attr :value job)
  [:input#org] (html/set-attr :value org)
  [:input#uuid] (html/set-attr :value id)
  [:a.p-delete-btn] (html/set-attr :href (str "/deleteprof/" id))
  [:div.pphotos] (html/html-content (photosimage photos))
  [:div.photost] (html/append (str "( " (count photos) " )")))

(defsnippet aboutpage "public/about.html"
  [:div#about]
  [])

(defsnippet programpage "public/project.html"
  [:div#program]
  [])

(defsnippet photodetpage "public/deletephotos.html"
  [:div#photodet]
  [id pht]
  [:img.detphotos] (html/set-attr :src (str "/profiles/" id "/" pht))
  [:a.p-delete-btn] (html/set-attr :href (str "/deletephoto/" id "/" pht)))

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
        (validate (indexpage (searchform) (db/searchf "")))
        (loginpage)))
  (POST "/login-action" {params :params}
    (let [username (:username params)
          password (:password params)] 
      (if (not= 0 (count (db/login-f username)))
        (if (= password (apply :password (db/login-f username)))
              (do 
                (session/put! :username username)
                (indexpage (searchform) (db/searchf "")))
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
        (merge-photo photos pphoto qid)
        (validate (indexpage (addprofile (mes "Profile berhasil tersimpan!" "green")) '())))))
  (GET "/adduser" []
    (validate (indexpage (searchform) '()) (indexpage (adduser) '())))
  (POST "/adduser-action" [nuser pw upw padmin]
    (if (= upw pw)
      (if (empty? (db/login-f nuser))
        (do
          (db/adduser nuser pw (Integer/parseInt padmin))
          (validate (indexpage (adduser (mes "User telah terdaftar!" "green")) '())))
        (validate (indexpage (adduser (mes "User sudah terdaftar sebelumnya." "red")) '())))
      (validate (indexpage (adduser (mes "Password tidak cocok." "red")) '()))))
  
  (GET "/userslist" []
    (validate (indexpage (searchform) '()) (indexpage (userslist) '())))
  (GET "/edit/:nuser" [nuser]
    (validate (indexpage (searchform) '()) 
      (if (= 741 (apply :admin (db/login-f nuser)))
        (indexpage (editsuperuser nuser) '())
        (indexpage (edituser nuser) '()))))
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
        (validate (indexpage (searchform) '())))
      (validate (indexpage (searchform) '()))))

  (GET "/deleteprof/:id" [id]
    (do
      (db/delete-prof id)
      (delete-folder (str "resources/public/profiles/" id))
      (validate (indexpage (searchform) (db/searchf "")))))

  (GET "/editprof/:id" [id]
    (let [dat (first (db/searchid id))
          nama (:name dat)
          age (:age dat)
          address (:address dat)
          job (:job dat)
          org (:organisation dat)
          profpic (:profilephoto dat)
          kasus (:kasus dat)
          photos (read-string (:photos dat))
          id (:uuid dat)]
      (validate (indexpage (editprofile profpic nama age address kasus job org photos id) '()))))
  (POST "/editprofile-action" {params :params}
    (let [profilephoto (:profilephoto params)
          nama (:name params)
          age (Integer/parseInt (:age params))
          address (:address params)
          kasus (:kasus params)
          job (:job params)
          org (:org params)
          photos (:photos params)
          id (:uuid params)]
      (do 
        (if (not (empty? (:filename profilephoto)))
          (do
            (up-file (str "resources/public/profiles/" id) profilephoto)
            (db/update-by-id id {:profilephoto (str "/profiles/" id "/"(:filename profilephoto))})))
        (up-file-multiple (str "resources/public/profiles/" id "/") photos)
        (update-file-multiple (str "resources/public/profiles/" id "/") photos id)
        (db/update-by-id id {:name nama :age age :address address :kasus kasus :job job :organisation org})
        (merge-photo (apply :photos (db/searchid id)) (apply :profilephoto (db/searchid id)) id)
        (resp/redirect (str "/profile/" id)))))
  (GET "/about" []
    (indexpage (aboutpage) '()))
  (GET "/program" []
    (indexpage (programpage) '()))
  (GET "/photodet/profiles/:id/:pht" [id pht]
    (validate (indexpage (photodetpage id pht) '())))
  (GET "/deletephoto/:id/:pht" [id pht]
    (if (not (empty? (session/get :username)))
          (do 
            (db/delete-pht id pht)
            (resp/redirect (str "/profile/" id)))))
  (POST "/addphoto-action/:id" {params :params}
    (let [id (:id params)
          photos (:photos params)
          pth (str "resources/public/profiles/" id "/")] 
      (do 
        (up-file-multiple (str "resources/public/profiles/" id "/") photos)
        (update-file-multiple (str "resources/public/profiles/" id "/") photos id)
        (resp/redirect (str "/profile/" id)))))

  (GET "/logout" []
    (do 
      (session/clear!)
      (loginpage)))
  (route/not-found "Not Found"))