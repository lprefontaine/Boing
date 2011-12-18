(ns boing.core.reflector
  "Reflection utility routines"
  (:require [clojure.string :as s])
  (:use
    [clojure.pprint] [clojure.stacktrace]
    [clojure.contrib.def])
  (:import [java.lang.reflect Modifier InvocationTargetException] [boing.core Util]))


(defvar- +setter-prefixes+ ["set" "add"])
(defvar- +array-fn+
  {
   (Class/forName "[Ljava.lang.String;") (fn [s] (into-array String s))
   (Class/forName "[I") (fn [s] (int-array s))
   (Class/forName "[J") (fn [s] (long-array s))
   (Class/forName "[S") (fn [s] (short-array s))
   (Class/forName "[B") (fn [s] (byte-array s))
   (Class/forName "[C") (fn [s] (char-array s))
   (Class/forName "[Z") (fn [s] (boolean-array s))
   (Class/forName "[F") (fn [s] (float-array s))
   (Class/forName "[D") (fn [s] (double-array s))
   })


(defvar- *reflection-cache* (atom {}))

(defn- get-reflection-info
  "Extract from the cache the required item for the given class."
  [java-class item]
  (try
    (let [hashcode (System/identityHashCode java-class)]
      (if-let [item-cached (item (get @*reflection-cache* hashcode))]
        item-cached
        (item (get (swap! *reflection-cache* #(merge %1 %2)
                          {hashcode (reduce merge {} (bean java-class))}) hashcode))))
    (catch Exception e# (print-cause-trace e#))))

(defn- update-reflection-info
  "Update the cache for the given class and returns the added map entry."
  [java-class item values]
  (let [hashcode (System/identityHashCode java-class)
        entry {item values}]
    (swap! *reflection-cache* #(merge %1 %2) {hashcode (merge (get *reflection-cache* java-class) entry)})
    entry))

  "Returns true if a class implements the given interface"
(defn- has-interface?
  [java-class ifc]
  (if-let [ifs (get-reflection-info java-class :interfaces)]
    (get ifs ifc)
    (loop [ifs (transient {}) curr-class java-class]
      (if (nil? curr-class) (persistent! ifs)
        (recur (reduce conj! ifs (map (fn [ifc] {ifc true}) (get-reflection-info java-class :interfaces)))
               (get-reflection-info java-class :superclass))))))

(defprotocol BoingReflector
  "This protocol helps the reflector to adapt Clojure values to java setter argument signatures."
  (to-java-arg [this target-class]))

(extend-type clojure.lang.LazySeq
  BoingReflector
  (to-java-arg
    [this target-class]
    (let [fn-array (get +array-fn+ target-class)]
      (cond
        fn-array (fn-array this)
        (has-interface? target-class java.util.List) (doall this)
        (has-interface? target-class java.util.Map) (doall this)
        :else (object-array (doall this))))))

(extend-type clojure.lang.PersistentVector
  BoingReflector
  (to-java-arg
    [this target-class]
    (let [fn-array (get +array-fn+ target-class)]
      (cond
        (= target-class java.util.Vector) (java.util.Vector. this)
        fn-array (fn-array this)
        :else this))))

(extend-type clojure.lang.PersistentList
  BoingReflector
  (to-java-arg
    [this target-class]
    (let [fn-array (get +array-fn+ target-class)]
      (cond
        fn-array (fn-array this)
        :else this))))

(defn- map-to-properties [m]
  (let [java-props (java.util.Properties.)]
    (dorun (map (fn [e] (.put java-props (name (key e)) (val e))) m))
    java-props))

(extend-type clojure.lang.PersistentHashMap
  BoingReflector
  (to-java-arg
    [this target-class]
    (cond
      (= target-class java.util.Properties) (map-to-properties this)
      :else this)))

(extend-type clojure.lang.PersistentArrayMap
  BoingReflector
  (to-java-arg
    [this target-class]
    (cond
      (= target-class java.util.Properties) (map-to-properties this)
      :else this)))

(extend-type Object
  BoingReflector
  (to-java-arg [this target-class] this))

(defn- setter-to-prop
  "Derive the property name from the setter name"
  [setter]
  (let [prop-name (first (remove nil? (map (fn [s] (if (.startsWith setter s) (s/replace-first setter s ""))) +setter-prefixes+)))]
  (keyword (str (s/lower-case (str (first prop-name ))) (apply str (rest prop-name))))))

(defn- setter?
  "Returns the first method name that qualifies as a setter."
  [mth-name]
  (first (remove false? (map (fn [s] (.startsWith mth-name s)) +setter-prefixes+))))

(defn- find-class-setters
  "Return all the setter methods for the given class as a hash indexed by the property name,
   the static setters are not retained. Setter methods are wrapped in a function.
   The setter's argument class is added to the function object for future validation.
   We assume that there is only one setter per property. This may be too simplistic however.
   Next release will index by property name and signature."
  [java-class]
  (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2))  (transient {})
                       (map (fn [mth]
                              (let [properties (bean mth)
                                    modifiers (:modifiers properties)
                                    mth-name (:name properties)
                                    static? (pos? (bit-and modifiers Modifier/STATIC))]
                                (cond
                                  static? {}
                                  (setter? mth-name)
                                  { (setter-to-prop mth-name) mth}
                                  :else {})))
                            (get-reflection-info java-class :declaredMethods)))))

(defn find-setters
  "Return all the setter methods for this class and its super classes
   as a hash indexed by the property name, the static setters are not retained
   If we already computed the setter map for this class, it's in the cache so we can return it immediately"
  [java-class]
  (if-let [setter-map (get-reflection-info java-class :setters)]
    setter-map
    (loop [setter-map (transient {}) curr-class java-class]
      (if (nil? curr-class)
        (:setters (update-reflection-info java-class :setters (persistent! setter-map)))
        (recur (reduce conj! setter-map (find-class-setters curr-class)) (:superclass (bean curr-class)))))))


(defn- valid-args?
  "Validate if the arguments are matching the signature.
   The arguments can be expressed as values or classes.
   We allow Map and List interfaces to match and can we defer validation later
   when needed."
  [java-class args mth-sig]
  (cond
    (and (zero? (count args)) (zero? (count mth-sig))) true
    (= (count args) (count mth-sig))
    (reduce #(and %1 %2)
            (map (fn [arg mth-class]
                   (if (nil? arg) true
                     (let [arg (if (class? arg) arg (to-java-arg arg mth-class))
                           arg-class (if (class? arg) arg (class arg))]
                       (cond
                             (keyword? arg) true ;; Maybe a reference to another object
                             (fn? arg) true      ;; Defer
                             (and (list? arg) (or (= mth-class java.util.List) (has-interface? mth-class java.util.List))) true
                             (and (map? arg) (or (= mth-class java.util.Map) (has-interface? mth-class java.util.Map))) true
                             (= (Util/getPrimitiveClass arg-class) mth-class) true
                             (= (class arg) mth-class) true
                             :else false)))) args mth-sig))
    :else false))

(defn- filter-class-methods
  "Filter methods matching signatures in the given class."
  ([java-class args]
    (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient [])
            (map (fn [mth]
                   (if (= (.getDeclaringClass mth) java-class)
                     (if (valid-args? java-class args (apply vector (.getParameterTypes mth))) mth)))
                 (get-reflection-info java-class :declaredMethods)))))
  ([java-class args mth-name]
    (let [pattern (re-pattern mth-name)]
      (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient [])
              (map (fn [mth]
                     (if (= (.getDeclaringClass mth) java-class)
                       (if (re-find pattern (.getName mth))
                         (if (valid-args? java-class args (apply vector (.getParameterTypes mth))) mth))))
                   (get-reflection-info java-class :declaredMethods)))))))

(defn find-methods
  "Find methods matching a signature for the given args including in super classes."
  ([java-class args]
    (loop [cl java-class mths (transient [])]
      (if (= cl Object) (persistent! mths)
        (recur (get-reflection-info cl :superclass) (reduce conj! mths (filter-class-methods cl args))))))
  ([java-class args mth-name]
    (loop [cl java-class mths (transient [])]
      (if (= cl Object) (persistent! mths)
        (recur (get-reflection-info cl :superclass) (reduce conj! mths (filter-class-methods cl args mth-name)))))))

(defn valid-method-sig?
  [java-class mth args]
  (valid-args? java-class args (apply vector (.getParameterTypes mth))))

(defn find-constructors
  "Find constructors matching a signature for the given args."
  [java-class args]
  (try
    (loop [cl java-class ctors (transient [])]
      (if (= cl Object) (persistent! ctors)
        (recur (get-reflection-info cl :superclass)
               (reduce #(if (nil? %2) %1 (conj! %1 %2)) ctors
                       (map (fn [ctor]
                              (if (= (.getDeclaringClass ctor) cl)
                                (if (valid-args? java-class args (apply vector (.getParameterTypes ctor))) ctor)))
                            (get-reflection-info cl :declaredConstructors))))))
    (catch Exception e# (print-cause-trace e#))))

(defn invoke-constructor
  "Invoke constructor with the given args."
  ([ctor args]
    (let [arg-classes (apply vector (.getParameterTypes ctor))
          args (to-array (map #(if (nil? %1) nil (to-java-arg %1 %2)) args arg-classes))]
      (try
        (.newInstance ctor args)
        (catch InvocationTargetException e#
          (print-cause-trace e#)
          (cond
            (instance? e# Exception) (throw (Exception. (.getCause e#)))
            (instance? e# Error) (throw (Error. (.getCause e#))))))))
  ([ctor]
    (try
      (.newInstance ctor (to-array []))
      (catch InvocationTargetException e#
        (print-cause-trace e#)
		      (cond
		        (instance? e# Exception) (throw (Exception. (.getCause e#)))
		        (instance? e# Error) (throw (Error. (.getCause e#))))))))

(defn invoke-method
  "Invoke the given method with the given values"
  ([instance method args args-classes]
    (try
      (let [args (to-array (map #(if (nil? %1) nil (to-java-arg %1 %2)) args args-classes))]
	      (.invoke method instance args))
      (catch InvocationTargetException e#
        (cond
          (instance? e# Exception) (throw (Exception. (.getCause e#)))
          (instance? e# Error) (throw (Error. (.getCause e#)))))))

  ([instance method]
    (try
      (.invoke method instance (to-array []))
      (catch InvocationTargetException e#
        (cond
          (instance? e# Exception) (throw (Exception. (.getCause e#)))
          (instance? e# Error) (throw (Error. (.getCause e#))))))))


