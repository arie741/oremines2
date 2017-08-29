(ns oremines2.db
  (:require [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]))

(def db2 (pg/pool :host "localhost:5432"
                  :user "oremines2"
                  :dbname "oremines2"
                  :password "admin2017"))

(defn alluser []
	(jdbc/query db2 [(str "select * from users")]))

(defn login-f [user]
	(jdbc/query db2 [(str "select * from users where username = '" user "'")]))

(defn searchf [prof]
	(jdbc/query db2 [(str "select * from profiles where name like '%" prof "%'")]))

(defn searchid [id]
	(jdbc/query db2 [(str "select * from profiles where uuid = '" id "'")]))

(defn admin? [username]
	(or (= 741 (:admin (first (jdbc/query db2 [(str "select admin from users where username = '" username "'")]))))
		(= 1 (:admin (first (jdbc/query db2 [(str "select admin from users where username = '" username "'")]))))))

;UPDATE 
(defn addprofdb [nm age sex pendidikan jurusan email phone kode keterangan r i a s e c photos profilephoto uuid]
	(jdbc/insert! db2 :profiles 
		{:name nm :age age :sex sex :pendidikan pendidikan :jurusan jurusan :email email :phone phone :kode kode :keterangan keterangan :r r :i i :a a :s s :e e :c c :photos photos :profilephoto profilephoto :uuid uuid}))

(defn adduser [nuser pw adm]
	(jdbc/insert! db2 :users
		{:username nuser :password pw :admin adm}))

(defn update-by-id [id params]
	(jdbc/update! db2 :profiles
		params ["uuid = ?" id]))

(defn update-by-user [nuser pw adm]
	(jdbc/update! db2 :users
		{:password pw :admin adm} ["username = ?" nuser]))

;DELETE
(defn delete-by-user [nuser]
	(jdbc/delete! db2 :users ["username = ?" nuser]))

(defn delete-prof [uuid]
	(jdbc/delete! db2 :profiles ["uuid = ?" uuid]))

(defn delete-pht [id pht]
	(let [photos (read-string (apply :photos (searchid id)))
		  upht (apply vector (remove #(= (str "/profiles/" id "/" pht) %) photos))]
		(jdbc/update! db2 :profiles
			{:photos (str upht)} ["uuid = ?" id])))

;;IP
(defn get-ip []
	(apply :address (jdbc/query db2 ["select * from ip"])))

(defn change-ip [ip]
	(jdbc/update! db2 :ip
		{:address ip} ["address like '%'"]))

(import (java.net NetworkInterface))
(def ip (.getHostAddress (java.net.InetAddress/getLocalHost)))