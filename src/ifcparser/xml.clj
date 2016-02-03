(ns ifcparser.xml
  (:require [clojure.xml :as xml]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [clojure.string :as s]
            [clojure.data.xml :as dxml]
            [clojure.data.zip.xml :as zip-xml]))

(defn ppxml- [xml]
   (let [in (javax.xml.transform.stream.StreamSource.
             (java.io.StringReader. xml))
         writer (java.io.StringWriter.)
         out (javax.xml.transform.stream.StreamResult. writer)
         transformer (.newTransformer
                      (javax.xml.transform.TransformerFactory/newInstance))]
     (.setOutputProperty transformer
                         javax.xml.transform.OutputKeys/INDENT "yes")
     (.setOutputProperty transformer
                         "{http://xml.apache.org/xslt}indent-amount" "3")
     (.setOutputProperty transformer
                         javax.xml.transform.OutputKeys/METHOD "xml")
     (.transform transformer in out)
     (-> out .getWriter .toString)))

(defn ppxml [xmlstr]
  (-> xmlstr
      (s/replace "\n" "")
      ppxml-))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (str text)
    (replace "&"  "&amp;")
    (replace "<"  "&lt;")
    (replace ">"  "&gt;")
    (replace "\"" "&quot;")))

(defn zip-str [s]
  (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

;;(def ss (zip-str (slurp "e.xml")))

;;(def ifclist (zip-xml/xml-> ss :ServiceProfile :InitialFilterCriteria))

(def x2 "    <InitialFilterCriteria>
      <Priority>4</Priority>
      <TriggerPoint>
        <ConditionTypeCNF>0</ConditionTypeCNF>
        <SPT>
          <ConditionNegated>0</ConditionNegated>
          <Group>0</Group>
          <Method>MESSAGE</Method>
        </SPT>
        <SPT>
          <ConditionNegated>0</ConditionNegated>
          <Group>0</Group>
          <SessionCase>0</SessionCase>
        </SPT>
        <SPT>
          <ConditionNegated>0</ConditionNegated>
          <Group>1</Group>
          <SessionDescription>
            <Line>m</Line>
            <Content>message</Content>
          </SessionDescription>
        </SPT>
        <SPT>
          <ConditionNegated>0</ConditionNegated>
          <Group>1</Group>
          <SessionCase>0</SessionCase>
        </SPT>
        <SPT>
          <ConditionNegated>0</ConditionNegated>
          <Group>1</Group>
          <Method>INVITE</Method>
        </SPT>
        <SPT>
          <ConditionNegated>1</ConditionNegated>
          <Group>0</Group>
          <SIPHeader>
            <Header>Route</Header>
            <Content>skipIFC</Content>
          </SIPHeader>
        </SPT>
        <SPT>
          <ConditionNegated>1</ConditionNegated>
          <Group>1</Group>
          <SIPHeader>
            <Header>Route</Header>
            <Content>skipIFC</Content>
          </SIPHeader>
        </SPT>
      </TriggerPoint>
      <ApplicationServer>
        <ServerName>sip:m-rms.ims.mts.ru;orig</ServerName>
        <DefaultHandling>0</DefaultHandling>
      </ApplicationServer>
    </InitialFilterCriteria>
")

(def i2 (zip-str x2))

(def scmap {"0" "ORIGINATING_SESSION"
            "1" "TERMINATING_REGISTERED"
            "2" "TERMINATING_UNREGISTERED"
            "3" "ORIGINATING_UNREGISTERED"
            "4" "ORIGINATING_CDIV"
            })

(def dhmap {"0" "SESSION_CONTINUED"
            "1" "SESSION_TERMINATED"})

(defn sipheader->map [sh]
  (str "Header=" (pr-str(first (zip-xml/xml-> sh :Header zip-xml/text)))
   " Content=" (pr-str (first (zip-xml/xml-> sh :Content zip-xml/text)))))

(defn sessdesc->map [sh]
  (str "Line=" (pr-str (first (zip-xml/xml-> sh :Line zip-xml/text)))
   " Content=" (pr-str (first (zip-xml/xml-> sh :Content zip-xml/text)))))

(defn sipmethod [sm]
  (str "Method="(pr-str (zip-xml/text sm))))

(defn sesscase [sc]
  (str "SessionCase=" (get scmap (zip-xml/text sc))))

(defn spt->map [sp]
  (let [group (first (zip-xml/xml-> sp :Group zip-xml/text))
        neg (first (zip-xml/xml-> sp :ConditionNegated zip-xml/text))
   spt {
   :method (zip-xml/xml-> sp :Method sipmethod)
   :sipheader (zip-xml/xml-> sp :SIPHeader sipheader->map)
   :sessdesc (zip-xml/xml-> sp :SessionDescription sessdesc->map)
   :sessioncase (zip-xml/xml-> sp :SessionCase sesscase)
   :requesturi (zip-xml/xml-> sp :RequestURI zip-xml/text)
   }
   message (first (remove empty? (vals spt)))]
    {:group group
     :string (if (= neg "1") (str "(NOT " (first message) ")") (first message))}
;     :string (str neg (first (remove empty? (vals spt))))}
    ))

(defn tp->map [tp]
  (let [tpl {:CNF (first (zip-xml/xml-> tp :ConditionTypeCNF zip-xml/text))
   :spts (mapv spt->map (zip-xml/xml-> tp :SPT))
   }
        groups (reduce (fn [c x] (conj c (:group x))) #{} (:spts tpl))]
    tpl
  ))

(defn print-tp [tp]
  (let [[op1 op2] (if (= (:CNF tp) "1") [" AND " " OR "] [" OR " " AND "])
        groups (reduce (fn [c x] (conj c (:group x))) (sorted-set) (:spts tp))
        ]
      (s/join op1 (for [y groups]
      (str "(" (s/join op2 (for [x (:spts tp) :when (= y (:group x))] (:string x))) ")")
      ))))

(defn print-as [as]
  (let [ass {:servername (first (zip-xml/xml-> as :ServerName zip-xml/text))
             :defhandling (get dhmap (first (zip-xml/xml-> as :DefaultHandling zip-xml/text)))}] ass)
)

(defn print-ifc [ifc]
  (let [tps (map tp->map (zip-xml/xml-> ifc :TriggerPoint))
        ifcmap {:priority (first (zip-xml/xml-> ifc :Priority zip-xml/text))
                :prtind (first (zip-xml/xml-> ifc :ProfilePartIndicator zip-xml/text))
                :tp (first (map print-tp tps))
                :src (escape-html (ppxml (with-out-str (xml/emit-element (zip/node ifc)))))}]
    (assoc ifcmap :as (print-as (zip-xml/xml1-> ifc :ApplicationServer)))
))

(defn decodeifcs [xmlstr]
  (let [ifcs (for [x (xml-seq (dxml/parse-str xmlstr)) :when (= :InitialFilterCriteria (:tag x))] (zip/xml-zip x))]
    (map print-ifc ifcs)))

;(def i1 (nth ifclist 3))
;(def tpp (map tp->map (zip-xml/xml-> i1 :TriggerPoint)))


;(map print-ifc ifclist)
;(def hr (print-ifc (nth ifclist 3)))
;hr
;(print hr)
;(nth ifclist 3)

;(first ())

;(def r (-> "example.nzb" io/resource io/file xml/parse zip/xml-zip))

;(def p (-> "e.xml" io/resource io/file xml/parse))
;(def p1 (slurp "e.xml"))

;(def ps (xml-seq p))

;(def ifcs (for [x ps :when (= :InitialFilterCriteria (:tag x))] x))
;(second (:content (first ifcs)))
;(def i3 (for [x (xml-seq (dxml/parse-str p1)) :when (= :InitialFilterCriteria (:tag x))] x))
;(print (ppxml (s/replace (with-out-str (xml/emit-element (first i3))) "\n" "")))

;(for [i i3]
;  (-> i
;      zip/xml-zip
;      print-ifc)
;  )

;(with-open [wrtr (io/writer "/tmp/t.xml")]
;  (.write wrtr (ppxml (s/replace (with-out-str (xml/emit-element (zip/node ss))) "\n" ""))))

;(def am ["a" "b" "c"])
;(for [[k v] (zipmap am (range))] v)

;i3

;(first i3)

;(-> (with-out-str (xml/emit-element (first i3)))
;      (s/replace "\n" "")
;      ppxml
;      (s/replace-first "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" "")
;    )
