(defproject oremines2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [compojure "1.5.1"]
                 [ring/ring-defaults "0.2.1"]
                 [clj-postgresql "0.4.0"]
                 [enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 [lib-noir "0.9.9"]
                 [dk.ative/docjure "1.11.0"]]  
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler oremines2.handler/app
         :auto-reload? true
         :auto-refresh? true
         :reload-paths ["src/" "resources/"]}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.3.0"]]}})
