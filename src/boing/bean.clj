(ns boing.bean
  "Implement bean definitions and instanciations.
   Beans can be defined using defbean or defabean.
   Bean definitions trigger a search for constructors and setters of the given class.
   Instanciation of a bean can be done using create-bean on a bean definition."
  (:use [boing.core.reflector] [boing.context] [boing.resource] [clojure.tools.trace] [clojure.stacktrace])
  (:require [clojure.string :as s])
  (:gen-class :name boing.Bean
              :methods [#^{:static true} [loadBeandefs [Object] void]
                        #^{:static true} [loadBeandefs [Object Object] void]                  
                        #^{:static true} [createBean [String] Object]
                        #^{:static true} [createBean [String java.util.List] Object]
                        #^{:static true} [createBean [String java.util.List java.util.List] Object]
                        #^{:static true} [createBeanFromContext [String String] Object]
                        #^{:static true} [createBeanFromContext [String String java.util.List] Object]
                        #^{:static true} [createBeanFromContext [String String java.util.List java.util.List] Object]
                        #^{:static true} [createSingletons [] java.util.Map]
                        #^{:static true} [createSingletons [java.util.List] java.util.Map]
                        #^{:static true} [createSingletons [String java.util.List] java.util.Map]
                        #^{:static true} [setDebug [java.lang.Boolean] void]]))

(def ^{:private true :dynamic true} *runtime-overrides* {})
(def ^{:private true :dynamic true} *aliases* {})
(def ^{:private true :dynamic true} *current-bean* nil)
(def ^{:private true :dynamic true} *depth* 0)
(def ^{:private true :dynamic true} *debug-mode* (atom false))
(def ^{:private true :dynamic true} *singletons* (atom {}))
(def ^{:private true :dynamic true} *post-clojure12*
  (cond (and (= (:major *clojure-version*) 1) (<= (:minor *clojure-version*) 2)) false
        :else true))

(defn jbyte
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to long."
  [x] (if *post-clojure12* (Byte. (byte x)) (byte x)))

(defn jshort
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to long."
  [x] (if *post-clojure12* (Short. (short x)) (short x)))

(defn jint
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to long."  
  [x] (if *post-clojure12* (Integer. (int x)) (int x)))

(defn jlong
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to long." 
  [x] (long x))

(defn jfloat
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to double." 
  [x] (if *post-clojure12* (Float. (float x)) (float x)))

(defn jdouble
  "Fn to hide 1.2 vs 1.3 numeric changes, avoid promotion to double." 
  [x] (double x))

(declare allocate-bean)

(defrecord Bean [context id java-class mode setters constructor
                 c-args init pre post class-override comments])
(defrecord BeanProperty
  [setter bean-id property palias value mth-arg-classes ])

(defprotocol BoingBean
  "Unifies a few methods between bean/non-bean objects."
  (get-value [this])
  (get-class [this]))

(extend-type Bean
  BoingBean
  (get-value [this] (allocate-bean this))
  (get-class [this] (let [{:keys [java-class class-override]} this]
                      (if (nil? class-override) java-class class-override))))

(extend-type clojure.lang.Keyword
  BoingBean
  (get-value [this]
    (if-let [beandef (find-beandef this)] ;; We need to fetch the bean definition from the current context.
      (allocate-bean beandef)             ;; and allocate it
      (throw (Exception. (format "Cannot find bean %s in context %s" this *current-context*)))))
  (get-class [this] clojure.lang.Keyword))

(extend-type clojure.lang.Fn
  BoingBean
  (get-value [this]
    (try
      (this)               ;; We need to call the closure to obtain the value to assign
      (catch Exception e# (throw e#))))
  (get-class [this] clojure.lang.AFn))

(extend-type Object        ;; Default implementation
  BoingBean
  (get-value [this] this)
  (get-class [this] (.getClass this)))

(defn- indent-level
  "Return an indent string according to the trace depth"
  [] (str *depth* " " (apply str (take (* *depth* 1) (cycle "|")))))

(defn- print-debug
  "Local trace fn"
  [& args] (println (str (indent-level) (apply format args))))

(defn- print-instance
  "Print instance summary if trace on."
  [id instance]
  (if @*debug-mode*
    (if (nil? instance)
      (do (println (str (indent-level) (format "Bean %s Object nil" id))) instance)
      (do (println (str (indent-level) (format "Bean %s Class %s Object %s" id (class instance) (.hashCode instance)))) instance)))
  instance)

(defn- to-keyword [s] (keyword (s/trim (name s))))

(defn- singleton-name ([id] (keyword (str "singleton-" id "-" (name *current-context*)))))

(defn- singleton?
  "Check if the given bean id corresponds to a singleton in the cache for the current context.
   if yes returns it. Singletons are not context dependent."
  [beandef]
  (cond
    (= (:mode beandef) :singleton) (get @*singletons* (singleton-name (:id beandef)))
    :else nil))

(defn- register-singleton
  "Register a singleton in the cache in the current context."
  [id beandef instance]
  (if @*debug-mode* (print-debug "Registering singleton: %s %s" id (.hashCode instance)))
  (swap! *singletons* #(merge %1 %2) { (singleton-name id) {:beandef beandef :instance instance}}))
 
(defn- property-def
  "Define a property value to be set at instantiation time"
  [bean-id java-class setters properties pname]
  (let [property-name (or (pname *aliases*) pname)
        setter (property-name setters)
        property (pname properties)]
    (cond (nil? setter)
          (throw (Exception. (format "No setter for property %s in class %s:\navailable setters %s"
                                     property-name java-class setters)))
          :else
          (do
            (if (not (nil? property)) (valid-method-sig? java-class setter [property]))
            { property-name
             (BeanProperty. setter bean-id  property-name  pname  property
                            (into [] (.getParameterTypes setter)))}))))

(defn- valid-bean-setters?
  "Checks if a bean has all the necessary property setters
   for the given property map. Resolve the aliases if they are defined.
   Returns all the setters it found."
  [bean-id java-class properties]
  (let [setters (find-setters java-class)]
    (persistent!
      (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient {})
              (doall (map (fn [p] (property-def bean-id java-class setters properties p)) (keys properties)))))))

(defn- to-class
  "Classes in bean definitions can be expressed as classes, keywords or strings.
   This function normalizes these to Class objects."
  [java-class]
  (try
	  (cond
	    (class? java-class) java-class
	    (keyword?  java-class)
	    (Class/forName (name java-class))
	    (instance? String java-class)
	    (Class/forName (name java-class))
	    :else
	    (throw (Exception. (format "Cannot get a class from %s" java-class))))
   (catch ClassNotFoundException e# (throw (Exception. (format "Class not found exception: %s" java-class))))
   (catch Exception e# (throw (Exception. (format "Error loading class %s: %s" java-class (.getMessage e#)))))))

(defn defbean
  "Create a bean definition"
  ([bean-name jclass & {:keys [mode s-vals c-args init pre post class-override comments] :or {mode :prototype}}]
  (try
    (let [id (keyword bean-name)
          java-class (to-class jclass)
          setters (valid-bean-setters? id java-class s-vals)
          constructor (first (find-constructors java-class c-args))
          init-mth (if-not (nil? init) (first (find-methods java-class [] init)))
          beandef (Bean. *current-context* id java-class mode setters constructor c-args init-mth pre post class-override comments)]
      (if (nil? constructor) (throw (NoSuchMethodException. (format "No constructor found for bean %s matching %s" bean-name c-args)))
        (add-beandef beandef))
      beandef)
    (catch NoSuchMethodException e# (throw e#))   
    (catch Exception e# (println (format "Error detected in bean definitions %s: %s" bean-name (.getMessage e#)))
      (print-stack-trace e#)
      (root-cause e#) (throw (Exception. (.getCause e#)))))))

(defn defabean
  "Create an anonymous bean definition. The bean inherits an autogenerated id"
  [jclass & {:keys [mode s-vals c-args init pre post class-override comments]}]
  (defbean (keyword (gensym "anonymous")) jclass :mode mode :s-vals s-vals :c-args c-args :init init :pre pre :post post :class-override
    class-override :comments comments))

(defn override
  "Fetch value overrides for the given bean/property."
  ([property-name]
    (if-let [bean-bindings (*current-bean* *runtime-overrides*)]
      (property-name bean-bindings)))
  ([bean-id property-name]
    (if-let [bean-bindings (bean-id *runtime-overrides*)]
      (property-name bean-bindings))))
 
(defn- apply-setter
  "Apply a setter to an allocated bean, We recurse through create-bean but the recursion level
   is acceptable. Bean hierarchies are not very deep most of the time and the stack should not blow out.
   A setter value can be:
      - Another embedded bean definition
      - A function
      - A bean id (keyword) referring to another bean definition in the current context
      - A Java object to pass directly to the setter"
  [instance propdef]
  (let [bean-id (:bean-id propdef)
        property (:property propdef)
        palias (:palias propdef)
        value (:value propdef)
        setter (:setter propdef)
        mth-arg-classes (:mth-arg-classes propdef)
        value-override (override bean-id palias)]
    (if-let [value (if (not (nil? value-override)) value-override
                     (if (not (nil? value)) (get-value value)))]
      (try
        (if @*debug-mode* (print-debug (format "Setting property %s %s" property (nil? value))))
        (invoke-method instance setter [value] mth-arg-classes)
        (catch Exception e#
          (print-cause-trace e#)
          (throw (Exception.
                   (format "Error in setter: %s\n\tsetter %s\n\tvalue %s" (.getMessage e#) property value)))))
      (if @*debug-mode* (print-debug (format "Property %s is null" property ))))))

(declare bean-info)

(defmacro invoke-wrapper [beandef step & body]
  `(let [id# (:id ~beandef)
         context# (:context ~beandef)]
     (try
       (if @*debug-mode* (print-debug (format "Invoking step %s on %s" ~step id#)))
       ~@body
     (catch Exception e#
       (println (format "Failed invoking %s step on instance of bean %s in context %s error: %s"
                        ~step id# context# (.getMessage e#)))
       (print-cause-trace e#) (throw (Exception. (.getCause e#))))))) 

(defn- allocate-singleton
  "Test for singleton and instantiate it if necessary."
  [beandef]
  (if-let [singleton (singleton? beandef)]
    (let [id (:id beandef)
          instance (:instance singleton)]
      (when @*debug-mode*
        (print-debug (format "Returning singleton %s %s" id (.hashCode instance))))
      instance)))

(defn- allocate-bean
  "Instantiate an object from a bean definition"
  [beandef]
  (try
    (binding [*depth* (inc *depth*)]
      (if-let [singleton (allocate-singleton beandef)] singleton
        (let [{:keys [constructor c-args java-class setters pre post init id mode] } beandef
              instance (invoke-constructor constructor (map #(if (nil? %) nil (get-value %)) c-args))
              property-overrides (id *runtime-overrides*)]
          (if @*debug-mode* (print-debug (format "Instantiating bean %s" id)))
          (binding [*current-bean* id]
            (if-not (nil? pre) (invoke-wrapper beandef "pre-function" (pre instance)))
            ;; Apply setters excluding the ones for which an override is provided
            (dorun (map (fn [e] (invoke-wrapper beandef "Setter" (apply-setter instance (val e)))) (dissoc setters (keys property-overrides))))
            ;; Apply property overrides if any
            (if-let [setter-overrides (valid-bean-setters? id java-class property-overrides)]
              (dorun (map (fn [e] (invoke-wrapper beandef "Override setters" (apply-setter instance (val e)))) setter-overrides)))
            (if-not (nil? init) (invoke-wrapper beandef "Initialization" (invoke-method instance init)))
            (let [post-instance  (if-not (nil? post)       ;; The post fn return value is returned as the object instance
                                   (invoke-wrapper beandef "post-function" (post instance)) instance)]
              (if (= mode :singleton) (register-singleton id beandef post-instance))
              post-instance)))))
    (catch Exception e# (print-cause-trace e#) (throw (Exception. (.getCause e#))))))

(defn create-bean
  "Return a new instanciation of a bean or the existing singleton if it already exists.
   Either an inline bean definition can be provided or a bean id (keyword).
   The bean definition will be searched either in the current context.
   The current context can be switched using the with-context macro.
   if no context is specified, the bean definition is pulled from the :default context"
  ([beandef-or-id] (create-bean beandef-or-id {} {}))
  
  ([beandef-or-id bean-overrides] (create-bean beandef-or-id bean-overrides {}))

  ([beandef-or-id bean-overrides global-overrides]
    (try
      (let [bean-id (if (keyword? beandef-or-id) beandef-or-id (:id beandef-or-id))
            ctx (get-context *current-context*)]
        (binding [*runtime-overrides* (merge *runtime-overrides* {bean-id bean-overrides} global-overrides)]
          (if (nil? beandef-or-id) nil (print-instance bean-id (get-value beandef-or-id)))))
      (catch Exception e#
        (print-stack-trace e# 1)
        (root-cause e#) (throw e#)))))

(defn- print-bean-info [beandef]
  (let [{:keys [id context java-class mode override-class comments]} beandef]
    (println (format "Bean %s  context: %s  mode: %s  class: %s  class override: %s\nComments: %s"
                     id context mode java-class override-class comments))))
  
(defn bean-info
  "Dump information of a specific bean"
  ([id]
    (if-let [ctx (get-context *current-context*)]
      (if-let [beandef (id (:beandefs ctx))] (print-bean-info beandef)
        (println (format "No such bean %s" id)))))
  ([id ctx-id]
    (if-let [ctx (get-context ctx-id)]
      (if-let [beandef (id (:beandefs ctx))] (print-bean-info beandef)
        (println (format "No such bean %s" id))))))

(defn bean-summary
  "Dump a bean summary of the current context."
  ([]
    (if-let [ctx (get-context *current-context*)]
      (do (println (format "Summary of context %s" *current-context*))
        (dorun (map (fn [e] (println (format "Bean %s  %s\n%s\n" (key e) (:java-class (val e)) (:comments (val e)))))
                    (:beandefs ctx))))))
  ([ctx-id]
    (if-let [ctx (get-context ctx-id)]
      (do (println (format "Summary of context %s" ctx-id))
        (dorun (map (fn [e] (println (format "Bean %s  %s\n%s\n" (key e) (:java-class (val e)) (:comments (val e)))))
                    (:beandefs ctx)))))))

(defn create-singletons
  "Create all the singletons in the current context.
   This allows to debug eventual instantiation failures instead
   of waiting for a bean instatiation that may occur very late.
   Only global overrides can be provided when calling this fn."
  ([] (create-singletons {}))
  ([global-overrides]
    (try
      (let [ctx (get-context *current-context*)]
        (binding [*runtime-overrides* (merge *runtime-overrides* global-overrides)]
          (persistent!
            (reduce #(if (nil? %2) %1 (conj! %1 %2))
                    (transient {}) (map (fn [e] (if (= (:mode (val e)) :singleton) {(key e) (print-instance (key e) (allocate-bean (val e)))}))
                                        (:beandefs ctx))))))
      (catch Exception e# (print-stack-trace e# 1) (root-cause e#) (throw e#)))))

(defn call-method
  "Helper fn to call hidden methods. It uses the arguments signature to find the method."
  [this mth-name & args]
  (let [cl (class this)
        arg-classes (if (pos? (count args)) (into-array java.lang.Class (map #(class %) args)) nil)
        args-array (if (pos? (count args)) (to-array args) nil)
        mth (.getDeclaredMethod cl mth-name arg-classes)]
    (.invoke mth this args-array)))

(defmacro with-context
  "Using the given context evaluate the given body.
   Bean definitions in the body will be attached to the given context."
  ([ctx-id & body]
  `(do (add-context ~ctx-id)
     (binding [*current-context* ~ctx-id]
       ~@body))))


(defmacro with-context-aliases
  "Using the given context and aliases evaluate the given body.
   Bean definitions in the body will be attached to the given context."
  ([ctx-id aliases & body]
  `(do (add-context ~ctx-id)
     (binding [*current-context* ~ctx-id *aliases* ~aliases]
       ~@body))))
 
(defn munge-class [cl]
  (cond (class? cl) cl
        (string? cl) (Class/forName cl)
        (keyword? cl) (Class/forName (name cl))
        :else (throw (Exception. (format "Cannot convert %s to class" cl)))))

(defn auto-promote
  "Extends the BoingReflector protocol to auto promote Clojure arguments
   to Java alternatives."
  [extended-class target-class impl]
  (do (extend-type (munge-class extended-class) BoingReflector
        (to-java-arg
          [this target-class] (impl this target-class)))))

(defn- eval-beandefs
  "Evaluate bean definitions in the current context from the given resource(s).
   Since this is used from the Java API, we test multiple parameter types to
   act accordingly."
  [bean-resources]
  (cond (instance? String bean-resources) ;; Maybe a file name or URL string
        (load-and-eval bean-resources)
        (instance? java.net.URL bean-resources) ;; Strict URL
        (load-and-eval bean-resources)
        (instance? (Class/forName "[Ljava.lang.Object;") bean-resources) ;; An array of file names/string URLs and or URLs
        (doall (map #(load-and-eval %) bean-resources))
        (instance? (Class/forName "[Ljava.lang.String;") bean-resources) ;; An array of file names or string URLs
        (doall (map #(load-and-eval %) bean-resources))
        (instance? (Class/forName "[Ljava.net.URL;") bean-resources) ;; An array of URLs
        (doall (map #(load-and-eval %) bean-resources)) 
        :else (throw (Exception. (format "Cannot load Boing definitions from %s" bean-resources)))))  
  
(defn -loadBeandefs
  "Load bean definitions from a clojure resource type.
   The resource can be a file in the class path or a URL.
   It can be loaded in a named context if required.
   This is a Java entry point so Java caller can access Boing bean definitions.
   Context name has to be passed as a string since Clojure keywords are unknown to Java."
  ([bean-resources] (eval-beandefs bean-resources))
  ([ctx-name bean-resources] (with-context (to-keyword ctx-name)  (eval-beandefs bean-resources))))

(defn- to-string-map
  "Transform keyword keys in a map to strings."
  [cmap]
  (persistent! (reduce #(if (nil? %2) %1 (assoc! %1 (name (key %2)) (val %2))) (transient {}) cmap)))

(defn- to-keyword-map
  "Transform a Java override map to a Clojure map.
   For global overrides we need to recurse down one level since we may have a map of overrides for each bean.
   The second entry point here is to prevent recursing down one level when we convert local overrides for the bean
   we area instantiating."
  ([java-list]
    (if (nil? java-list) {}
      (persistent! (reduce #(if (nil? %2) %1 (assoc! %1 (to-keyword (key %2))
                                                     (to-keyword-map (val %2) true)))  (transient {}) (apply hash-map java-list)))))
  ([java-list one-level]
    (if (nil? java-list) {}
      (if-not (instance? java.util.List java-list) java-list
        (persistent! (reduce #(if (nil? %2) %1 (assoc! %1 (to-keyword (key %2)) (val %2)))  (transient {}) (apply hash-map java-list)))))))

(defn -createBeanFromContext
  "Instantiate a bean from a definition in a specific context
   Override Java maps are converted to Clojure maps if needed.
   This is a Java entry point so Java caller can instantiate Boing bean definitions.
   Context name and bean name have to be passed as a string since Clojure keywords are unknown to Java."
  ([^String ctx-name ^String bean-name] (with-context (to-keyword ctx-name) (create-bean (to-keyword bean-name))))
  ([^String ctx-name ^String bean-name ^java.util.List bean-overrides]
    (with-context (to-keyword ctx-name) (create-bean (to-keyword bean-name) (to-keyword-map bean-overrides true))))
  ([^String ctx-name ^String bean-name ^java.util.List bean-overrides ^java.util.List global-overrides]
    (with-context (to-keyword ctx-name)
      (create-bean (to-keyword bean-name) (to-keyword-map bean-overrides true)
                   (to-keyword-map global-overrides)))))

(defn -createBean
  "Instantiate a bean from a definition in the default context
   Override Java maps are converted to Clojure maps if needed.
   This is a Java entry point so Java caller can instantiate Boing bean definitions."

  ([^String bean-name] (create-bean (to-keyword bean-name)))
  ([^String bean-name ^java.util.List bean-overrides]
    (create-bean (to-keyword bean-name) (to-keyword-map bean-overrides true)))
  ([^String bean-name ^java.util.List bean-overrides ^java.util.List global-overrides]
    (create-bean (to-keyword bean-name)  (to-keyword-map bean-overrides true)
                 (to-keyword-map global-overrides))))

(defn -createSingletons
  "Instantiate all singletons in the current/given context.
   This is a Java entry point so Java caller can access Boing bean definitions.
   Context name has to be passed as a string since Clojure keywords are unknown to Java."
  ([] (to-string-map (create-singletons)))
  ([^java.util.List global-overrides] (to-string-map (create-singletons (to-keyword-map global-overrides))))
  ([^String ctx-name ^java.util.List global-overrides]
    (to-string-map (with-context (to-keyword ctx-name) (create-singletons (to-keyword-map global-overrides))))))

(defn set-debug!
  "Set/reset debug mode."
  [debug] (reset! *debug-mode* debug))

(defn -setDebug "From Java..." [^java.lang.Boolean debug] (reset! *debug-mode* debug))

