(ns ifcparser.handler
  (:gen-class)
  (:use         compojure.handler
                compojure.core
                [ring.middleware file-info file]
                )
  (:require [compojure.core :refer [defroutes]]
            ;;            [shriek.routes.home :refer [home-routes app-rroutes ws-routes ch-chsk event-msg-handler]]
            ;;            [ifcparser.middleware :as middleware]
            [noir.util.middleware :refer [app-handler wrap-access-rules]]
            [noir.session :as session]
            [noir.response :as resp]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor]
            [ring.middleware.reload :as reload]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [ring.middleware.anti-forgery :refer :all]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.defaults :refer :all]
            [environ.core :refer [env]]
            [ifcparser.xml :refer [decodeifcs]]
            [cheshire.core :refer :all]
            ))

(defn tee [data]
  (println (pr-str data))
  data)

(defn log-request [handler]
  (if (env :dev)
    (fn [req]
      (timbre/debug req)
      (let [resp (handler req)]
        (timbre/debug resp)
        resp))
    handler))

(defn response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "init will be called once when
  app is deployed as a servlet on
  an app server such as Tomcat
  put any initialization code here"
  []
  (timbre/set-config!
   [:appenders :rotor]
   {:min-level :info
    :enabled? true
    :async? false ; should be always false for rotor
    :max-message-per-msecs nil
    :fn rotor/rotor-appender})

  (timbre/set-config!
   [:shared-appender-config :rotor]
   {:path "ifcparser.log" :max-size (* 512 1024) :backlog 10})

  (timbre/info "Shriek started successfully"))

(defn destroy
  "destroy will be called when your application
  shuts down, put any clean up code here"
  []
  (timbre/info "Shriek is shutting down..."))

(defn processxml [xmlstr]
  (try (response {:ok true :data (->> xmlstr
             decodeifcs
             (zipmap (range))
             )}
        )
    (catch Exception e (response {:ok false :message (.getMessage e)})))
  )

(defroutes home-routes
  (GET "/" [] (slurp "resources/public/html/index.html"))
  (GET "/token" [] (response {:csrf-token
                                *anti-forgery-token*}))
  (POST "/parse" [data] (tee (processxml data)))
  )

(defn get-custom-token [request]
  (get-in request [:headers "x-csrf-token"]))

(def app (app-handler
            [home-routes app-routes]
            :middleware [
              ;  log-request
               wrap-edn-params
               wrap-anti-forgery
               ;;                       middleware/template-error-page
             ]
            :access-rules []
            :format []
            :ring-defaults (assoc-in site-defaults [:security :anti-forgery] false)
          ))
