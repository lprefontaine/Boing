(ns boing.translator
  "Implement translation fom Spring Bean XML definitions to Boing definitions"
  (:require [clojure.xml :as xml])
  (:use [clojure.tools.trace]))

(def ^{:private true} DEFBEAN "(defbean")
(def ^{:private true} ENDBEAN ")")
(def ^{:private true} SPACE " ")
(def ^{:private true} EOL "\n")
(def ^{:private true} STR-QUOTE "\"")
(def ^{:private true} START-SETTERS ":s-vals {")
(def ^{:private true} END-SETTERS "}")
(def ^{:private true} START-CTOR ":c-args [")
(def ^{:private true} END-CTOR "]")

(defn ^{:private true} strq
  "String quote a string value"
  [s] (str STR-QUOTE s STR-QUOTE))

(defn ^{:private true} ins-space 
  "Add spaces between collection elements, returning a string."
  [coll] (apply str (interpose SPACE (trace (filter #(if-not (nil? %) %) coll)))))

(defn ^{:private true} ins-lbreak
  "Add EOL between collection elements, returning a string."
  [coll] (apply str (interpose EOL (filter #(if-not (nil? %) %) coll))))


(deftrace ^{:private true} parse-value
  "Parse the given xml expresses value.
If the value is a reference to another bean, just return the bean id as a keyword."
  [value]
  
  )

(defn ^{:private true} find-matching-setter
  "Find a matching setter for the given property class and value."
  [klass property value])


(defn ^{:private true} find-matching-ctor
  "Find a matching constructor for the given argumengt signature."
  [klass ctor-args])
   
   
(deftrace ^{:private true} create-ctor
  "Create the constructor values string if specified"
  [properties]
  (let [cleaned (doall (remove nil? properties))]
    (if (seq cleaned)
      (str START-CTOR
           (apply str
                  (ins-lbreak
                    (doall (map #(:value %) properties))))
           END-CTOR))))

(deftrace ^{:private true} create-setters-props
  "Create the setter values string."
  [properties]
  (let [cleaned (doall (remove nil? properties))]
    (if (seq cleaned)
      (str START-SETTERS
           (apply str
                  (ins-lbreak
                    (doall (map #(str (keyword (:name %)) SPACE (:value %)) properties))))
           END-SETTERS))))
  
(defn ^{:private true} create-boing-def
  "Create a boing definition as a string."
  [beandef]
  (let [atrs (:attrs beandef)
        id (:id atrs)
        cl (:class atrs)
        setter-props (trace (into [] (map #(if (= (:tag %) :property) (:attrs %)) (:content beandef))))
        ctor-props (into [] (map #(if (= (:tag %) :constructor-arg) (:attrs %)) (:content beandef)))]
    (apply str (ins-space [DEFBEAN (keyword id) (strq cl)
                 (create-ctor ctor-props) (create-setters-props setter-props)])
         ENDBEAN)))

(defn translate-spring-to-boing
  "Create boing definitions from an existing Spring xml file definition."
  [file]
  (let [beandefs (apply vector (filter #(if (= (:tag %) :bean) %) (:content (xml/parse file))))]
    (println (format "Parsing file %s: found %d bean definitions" file (count beandefs)))
    (str (apply str (ins-lbreak (map create-boing-def beandefs))) EOL)))

(defn -main [& argv]
  )


