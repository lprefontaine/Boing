(ns boing.translator
  "Implement translation fom Spring Bean XML definitions to Boing definitions"
  (:require [clojure.xml :as xml])
  (:use [boing.core.reflector] [clojure.tools.trace]))

(def ^{:private true} DEFBEAN "(defbean")
(def ^{:private true} ENDBEAN ")")
(def ^{:private true} SPACE " ")
(def ^{:private true} EOL "\n")
(def ^{:private true} STR-QUOTE "\"")
(def ^{:private true} START-SETTERS ":s-vals {")
(def ^{:private true} END-SETTERS "}")
(def ^{:private true} START-CTOR ":c-args [")
(def ^{:private true} END-CTOR "]")
(def ^{:private true} +strwrappers+
  {java.lang.Byte "(jbyte %s)"
   java.lang.Short "(jshort %s)"
   java.lang.Integer "(jint %s)"
   java.lang.Long "(jlong %s)"
   java.lang.Float "(jfloat %s)"
   java.lang.Double "(jdouble %s)"
   java.lang.Character "\\%s"
   java.lang.Boolean "%s"
   java.lang.String "\"%s\""})

(defn ^{:private true} strq
  "String quote a string value"
  [s] (str STR-QUOTE s STR-QUOTE))

(defn ^{:private true} ins-space 
  "Add spaces between collection elements, returning a string."
  [coll] (apply str (interpose SPACE (trace (remove nil? coll)))))

(deftrace ^{:private true} ins-lbreak
  "Add EOL between collection elements, returning a string."
  [coll] (trace coll)
  (apply str
         (interpose EOL
                    (remove nil? coll))))

(defn ^{:private true} test-matching-setter
  "Test a matching setter exists for the given property class and value."
  [klass property value]
  ;;valid-setter?
  )

(defn ^{:private true} parse-numeric
  [klass property value]
  (let [setter (trace (find-setter klass property))]
    (cond (re-matches #"[1-2]*[0-9]{1,2}" value)
          nil)))

(deftrace ^{:private true} parse-value
  "Parse the given xml expresses value.
If the value is a reference to another bean, just return the bean id as a keyword."
  [klass xml]
  (println (format "XML received %s" xml))
  (let [property (:name xml)
        refid (:ref xml)
        value (:value xml)]
    (cond (:ref value) (str (keyword property) SPACE (keyword refid) EOL)
          (re-matches #"[0-9]+" value) (str (keyword (:name value)) SPACE (parse-numeric klass property value) EOL)
          :else (str (keyword (:name value)) SPACE (strq value) EOL))))

(defn ^{:private true} test-matching-ctor
  "Test if a matching constructor exists for the given argumengt signature."
  [klass ctor-args])   
   
(deftrace ^{:private true} create-ctor
  "Create the constructor values string if specified"
  [klass properties]
  (if (seq properties)
    (str START-CTOR
         (apply str (ins-lbreak (doall (map #(parse-value klass (:value %)) properties))))
         END-CTOR)))


(deftrace ^{:private true} create-setters-props
  "Create the setter values string."
  [klass properties]
  (if (seq properties)
    (let [values (doall (map #(parse-value klass %1) properties))
          _ (doall (map #(test-matching-setter klass %1 %2) properties values))]
      (str START-SETTERS
           (apply str (ins-lbreak (doall (map #(str (keyword %1) SPACE %2) properties values))))
           END-SETTERS))))
  
(deftrace ^{:private true} create-boing-def
  "Create a boing definition as a string."
  [beandef]
  (let [atrs (:attrs beandef)
        id (:id atrs)
        cl (:class atrs)
        setter-props (doall (into [] (remove nil? (map #(if (= (:tag %) :property) (:attrs %)) (:content beandef)))))
        ctor-props (doall (into [] (remove nil? (map #(if (= (:tag %) :constructor-arg) (:attrs %)) (:content beandef)))))]
    (apply str (ins-space [DEFBEAN (keyword id) (strq cl)
                           (if (pos? (count setter-props)) (create-setters-props cl setter-props))
                           (if (pos? (count ctor-props)) (create-ctor cl ctor-props))])
           ENDBEAN)))

(defn translate-spring-to-boing
  "Create boing definitions from an existing Spring xml file definition."
  [file]
  (let [beandefs (apply vector (filter #(if (= (:tag %) :bean) %) (:content (xml/parse file))))]
    (println (format "Parsing file %s: found %d bean definitions" file (count beandefs)))
    (str (apply str (ins-lbreak (map create-boing-def beandefs))) EOL)))

(defn -main [& argv]
  )


