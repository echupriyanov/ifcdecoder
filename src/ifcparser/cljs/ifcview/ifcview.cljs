(ns ifcparser.ifcview
  (:require [enfocus.core :as ef]
            [clojure.string :as s]
            [ajax.core :refer [GET POST]]
            [ajax.edn :as edn]
            )
  (:require-macros [enfocus.macros :as em]))

(def sample_error "Line 1\nLine 2")

(defn ^:export toggle_src [id]
  (if (= (ef/from (str "#xmlSrc" id) (ef/get-attr :class)) "collapse")
    (ef/at (str "#xmlSrc" id) (ef/set-attr :class "collapse in"))
    (ef/at (str "#xmlSrc" id) (ef/set-attr :class "collapse"))

  ))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (str text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")))

(defn str2html [str]
  (s/replace str "\n" "<br />"))

(def a-token (atom ""))

; (defn str2html [str] str)

(em/defsnippet em-error "/html/snippets.html" "#error" [message]
  "#error" (ef/content (str2html message)))

(em/defsnippet em-inputform "/html/snippets.html" "#in-form" []
  "#btn-parse" (ef/set-attr :onclick "ifcparser.ifcview.try_parse_xml()"
                               ))
; (em/defsnippet em-inputform "/html/snippets.html" "#in-form" []
;   "#btn-parse" (ef/set-attr :onclick "ifcparser.ifcview.check_headers()"
;                                ))


(em/defsnippet em-ifc "/html/snippets.html" "#ifc-table" [[n ifc]]
  "#number" (ef/content (str n))
  "#priority" (ef/content (:priority ifc))
  "#partind" (ef/content (:partind ifc))
  "#tp" (ef/content (:tp ifc))
  "#as-name" (ef/content (get-in ifc [:as :servername]))
  "#defhandling" (ef/content (get-in ifc [:as :defhandling]))
;  "#showhide" (ef/set-attr :onclick (str "ifcparser.ifcview.toggle_src(" n ")"))
  "#showhide" (ef/set-attr :data-target (str "#xmlSrc" n))
  ".xmpxml" (ef/set-attr :id (str "xmlSrc" n))
  ".xmpxml" (ef/content  (:src ifc))
  )

(em/defsnippet em-ifc1 "html/snippets.html" "#ifc-table" [[n ifc]])

;;(defn err-handler [{:keys [status status-text]}]
(defn err-handler [r]
  (.log js/console (pr-str "Something bad happened: " r)))

(defn handler2 [data]
  (reset! a-token (:csrf-token data))
  (.log js/console (str "get was ok: " @a-token))
  )

(defn get-token []
  (GET "/token"
    {:handler handler2
     :response-format (edn/edn-response-format)
      }))

(defn ^:export check_headers []
  (GET "/ping" {
    :headers {"X-CSRF-Token" @a-token}
  }
  ))

(defn ^:export show-result [response]
  (if-not (:ok response)
    (ef/at "#result" (ef/content (em-error (str2html (:message response)))))
    (ef/at "#result" (ef/content (map em-ifc (:data response)))))
  )

(defn ^:export try-parse-xml []
  (.log js/console (str "Parse pressed! Form data: " (ef/from "#xmlinput" (ef/read-form-input))))
  (POST "/parse"
        {:params {:data (ef/from "#xmlinput" (ef/read-form-input))
                  :__anti-forgery-token @a-token}
         :handler show-result
         :error-handler err-handler
         :format (edn/edn-request-format)
         :response-format (edn/edn-response-format)
         :headers {"X-CSRF-Token" @a-token
                  "X-XSRF-Token" @a-token}
         }))

(defn ^:export start []
  (get-token)
  (ef/at "#main-content" (ef/content (em-inputform)))
  (.log js/console (pr-str "CSRF Token: " (GET "/token")))
  )

;; (set! (.-onload js/window) start)
(set! (.-onload js/window) #(em/wait-for-load (start)))
