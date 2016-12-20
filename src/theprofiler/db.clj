(ns theprofiler.db
  (:require [clj-postgresql.core :as pg]
            [clojure.java.jdbc :as jdbc]))

(def db2 (pg/pool :host "localhost:5432"
                  :user "theprofiler"
                  :dbname "theprofiler"
                  :password "admin2016"))

(defn login-f [user]
	(jdbc/query db2 [(str "select * from users where username = '" user "'")]))

(defn searchf [prof]
	(jdbc/query db2 [(str "select * from profiles where name like '%" prof "%'")]))

(defn searchid [id]
	(jdbc/query db2 [(str "select * from profiles where uuid = '" id "'")]))