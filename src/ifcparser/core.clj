(ns ifcparser.core
  (:gen-class)
  (:use [ifcparser.handler]
        [org.httpkit.server :only [run-server]]
        [ring.middleware file-info file]))


;; (defn -main
;;   "I don't do a whole lot ... yet."
;;   [& args]
;;   (println "Hello, World!"))

(defn -main [& args]
  (let [port (if (empty? args)
               3000
               (Integer/parseInt (first args)))
        ]
    (println "Starting the network server on port" port)
    (run-server (-> app
                    (wrap-file "resources")
                    wrap-file-info) {:port port})
    ))
