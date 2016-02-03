(defproject ifcparser "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/data.zip "0.1.1"]
                 [compojure "1.4.0"]
                 [lib-noir "0.9.9"]
                 [cljs-ajax "0.5.3"]
                 [environ "1.0.2"]
                 [ring/ring-defaults "0.1.5"]
                 [fogus/ring-edn "0.3.0"]
                 [http-kit "2.1.18"]
                 [enfocus "2.1.1"]
                 [cheshire "5.5.0"]
                 [com.taoensso/timbre "4.2.1"]
                 [com.taoensso/tower "3.0.2"]
                 [org.clojure/data.xml "0.0.8"]]

  :main ^:skip-aot ifcparser.core
  :target-path "target/%s"
  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-environ "1.0.2"]
            [lein-ring "0.9.7"]]
  :ring {:handler ifcparser.handler/app}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[ring/ring-mock "0.3.0"]
                                  [ring/ring-devel "1.4.0"]]
                   :env {:dev true}}}
:cljsbuild {
        :builds [
                  { :id "app"
                    :source-paths ["src/ifcparser/cljs/ifcview"]
                    :compiler {
                      :output-to     "resources/public/js/ifcview.js"
                      :output-dir    "resources/public/js/ifcview"
                      :optimizations :none
                      :source-map    true
                  }}
                 ]}
  )
