(ns boing.test.test-bean
  (:use [boing.bean] [boing.context] [clojure.test] [clojure.pprint]
        [clojure.contrib.trace]))

(deftest test-noargs-beandef []
  (testing
    "Testing bean definitions without args"
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass)) boing.test.SimpleClass))))

(deftest test-class-constructor-beandef []
  (testing
    "Testing bean definitions using only constructors and keyword class names"
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass)) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass
               :c-args [(byte 1)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string"])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 :boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true])) boing.test.SimpleClass))))

(deftest test-string-constructor-beandef []
  (testing
    "Testing bean definitions using only constructors and String class names"
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass")) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass"
               :c-args [(byte 1)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass"
               :c-args [(byte 1) (short 2) (int 3) (long 4)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass"
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string"])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass"
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 "boing.test.SimpleClass"
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true])) boing.test.SimpleClass))))

(deftest test-constructor-properties []
  (testing
    "Testing bean definitions using only constructors: boing.test.SimpleClass"
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass)) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string"])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2)])) boing.test.SimpleClass))
    (is (= (:java-class
             (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true])) boing.test.SimpleClass))))

(deftest test-constructor-w-options []
  (testing
    "Testing bean definitions with some options"
    (let [simple-bean (defbean :test-bean-1 boing.test.SimpleClass :pre (fn [x] )
                        :post (fn [x] ) :class-override Long)]
      (is (= (:class-override simple-bean) Long)))))


(deftest test-create-bean-constructors []
  (testing
    "Testing bean creationusing only constructors: boing.test.SimpleClass"
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass))) "0:0:0:0:null:0.0:0.0:\\u0000:false"))
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1)]))) "1:0:0:0:null:0.0:0.0:\\u0000:false"))
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4)]))) "1:2:3:4:null:0.0:0.0:\\u0000:false"))
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass
               :c-args [(byte 1) (short 2) (int 3) (long 4) "Test String"]))) "1:2:3:4:Test String:0.0:0.0:\\u0000:false" ))))

(deftest test-create-bean-w-setters []
  (testing
    "Testing bean creation using an empty constructor and setters: boing.test.SimpleClass"
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})))
          "1:2:3:4:This is a test:0.0:0.0:\\u0000:false"))
    (is (= (.toString
             (create-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})))
          "0:0:0:0:null:2.3:3.4:H:true"))))

(deftest test-complex-w-setters[]
    (testing
      "Testing bean creation referring to other beans"
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (:java-class
                 (defbean :test-bean-2 boing.test.ComplexClass
                   :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean})) boing.test.ComplexClass))))
    (testing
      "Testing beans creation referring to other anonymous beans"
      (let [first-bean (defabean boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defabean boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (:java-class
                 (defbean :test-bean-2 boing.test.ComplexClass
                   :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean})) boing.test.ComplexClass)))))
(deftest test-create-complex []
    (testing
      "Testing bean creation referring to other beans"
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (.toString
                 (create-bean (defbean :test-complex-bean-1 boing.test.ComplexClass
                   :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean}))) "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false"))))
    (testing
      "Testing beans creation referring to other anonymous beans"
      (let [first-bean (defabean boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defabean boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (.toString
                 (create-bean (defbean :test-complex-bean-2 boing.test.ComplexClass
                                :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean}))) "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false")))))

(deftest test-singleton-setters []
    (testing
      "Testing singleton creation using setters"
      (let [singleton (defbean :test-singleton-1 boing.test.SimpleClass :mode :singleton
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            alien-singleton
            (with-context :my-ctx
              (defbean :test-singleton-1 boing.test.SimpleClass :mode :singleton
                :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true}))]
        (is (= (.hashCode (create-bean singleton)) (.hashCode (create-bean singleton))))
        (with-context :my-ctx
          (is (= (.hashCode (create-bean alien-singleton)) (.hashCode (create-bean alien-singleton))))))))

(deftest test-singleton-constructor []
    (testing
      "Testing singleton creation using constructor"
      (let [singleton (defbean :test-singleton-1 boing.test.SimpleClass :mode :singleton
                         :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true])
            alien-singleton
            (with-context :my-ctx
              (defbean :test-singleton-1 boing.test.SimpleClass :mode :singleton
                :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true]))]
        (is (= (.hashCode (create-bean singleton)) (.hashCode (create-bean singleton))))
        (with-context :my-ctx
          (is (= (.hashCode (create-bean alien-singleton)) (.hashCode (create-bean alien-singleton)))))
        )))


(deftest test-setters-beanref []
    (testing
      "Testing bean creation with bean references using setters"
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (.toString
                 (create-bean (defbean :test-complex-bean-1 boing.test.ComplexClass
                                :s-vals {:simpleBeanOne :test-bean-1 :simpleBeanTwo :test-bean-2})))
              "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false")))))


(deftest test-constructor-beanref []
    (testing
      "Testing bean creation with bean references using constructor"
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]
        (is (= (.toString
                 (create-bean (defbean :test-complex-bean-1 boing.test.ComplexClass
                                :c-args [:test-bean-1 :test-bean-2])))
              "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false")))))

(deftest test-setters-closures []
    (testing
      "Test bean creation with closures using setters"
      (let [dbl 7.8
            nb 5
            first-bean (defabean boing.test.SimpleClass
                         :s-vals {:intVal (fn [] nb) :floatVal (float 2.3) :doubleVal (fn [] dbl) :charVal \H :boolVal true})]
        (is (= (.toString (create-bean first-bean)) "0:0:5:0:null:2.3:7.8:H:true")))))

(deftest test-constructor-closures []
    (testing
      "Test bean creation with closures using constructors"
      (let [dbl 7.8
            nb 5
            first-bean (defabean boing.test.SimpleClass
                         :c-args [(byte 0) (short 0) (fn [] nb) (long 0) "test" (float 2.3) (fn [] dbl) \H  true])]
        (is (= (.toString (create-bean first-bean)) "0:0:5:0:test:2.3:7.8:H:true")))))

(deftest test-setters-maps-and-lists []
    (testing
      "Test bean creation with maps and lists using setters"
      (let [listval (list 1 2 3)
            mapval { :a 1 :b 2}
            first-bean (defabean boing.test.SimpleClass
                         :s-vals {:listVal listval :mapVal mapval})]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:(1 2 3):{:a 1, :b 2}")))))


(deftest test-constructors-maps-and-lists []
    (testing
      "Test bean creation with maps and lists using constructors"
      (let [listval (list 1 2 3)
            mapval { :a 1 :b 2}
            first-bean (defabean boing.test.SimpleClass
                         :c-args [ listval mapval])]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:(1 2 3):{:a 1, :b 2}")))))


(deftest test-setters-properties-class []
    (testing
      "Test bean creation with Properties autopromotion using setters"
      (let [mapval { :a 1 :b 2}
            first-bean (defabean boing.test.SimpleClass
                         :s-vals {:props mapval})]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}")))))


(deftest test-constructors-properties-class []
    (testing
      "Test bean creation with Properties autopromotion using constructors"
      (let [mapval { :a 1 :b 2}
            first-bean (defabean boing.test.SimpleClass
                         :c-args [ mapval])]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}")))))

(deftest test-setters-vector-class []
    (testing
      "Test bean creation with Vector autopromotion using setters"
      (let [vecval [ 1 2 3 4 5]
            first-bean (defabean boing.test.SimpleClass
                         :s-vals {:vector vecval})]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:[1, 2, 3, 4, 5]")))))


(deftest test-constructors-vector-class []
    (testing
      "Test bean creation with Vector autopromotion using constructors"
      (let [vecval [ 1 2 3 4 5]
            first-bean (defabean boing.test.SimpleClass
                         :c-args [ vecval])]
        (is (= (.toString (create-bean first-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:[1, 2, 3, 4, 5]")))))

(deftest test-instantiate-all-singletons []
  (testing
    "Test instantiation of all singletons at once"
    (let [ctx (keyword (gensym "ctx-"))] ;; To make sure we have a fresh context at each test run
      (with-context ctx
        (let [singleton-bean-1 (defbean :s1 boing.test.SimpleClass :mode :singleton
                                 :c-args [ { :a 1 :b 2}])
              singleton-bean-2 (defbean :s2 boing.test.SimpleClass :mode :singleton
                                 :c-args [ { :a 3 :b 4}])
              singleton-bean-3 (defbean :s3 boing.test.SimpleClass :mode :singleton
                                 :c-args [ { :a 5 :b 6}])
              singletons (create-singletons)]
          (is (= (apply str singletons)
                 "[:s3 #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=6, a=5}>][:s2 #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=4, a=3}>][:s1 #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}>]")))))))

(deftest test-aliases []
  (testing
    "Test instantiation using property aliases"
    (let [ctx (keyword (gensym "ctx-"))]  ;; To make sure we have a fresh context at each test run
      (with-context-aliases ctx {:b :byteVal :i :intVal :f :floatVal}
        (let [first-bean (defbean :test-bean-2 boing.test.SimpleClass
                           :s-vals {:b (byte 1) :i (int 3) :f (float 4.3)})]
        (is (= (.toString (create-bean first-bean)) "1:0:3:0:null:4.3:0.0:\\u0000:false"))
        (is (= (.toString (create-bean first-bean {:b (byte 3)})) "3:0:3:0:null:4.3:0.0:\\u0000:false")))))))

(deftest test-auto-promotion []
  (testing
    "Test instantiation using property aliases"
    (let [ctx (keyword (gensym "ctx-"))]  ;; To make sure we have a fresh context at each test run
      (auto-promote
        boing.test.SimpleClass java.util.Properties
        (fn [this target-class]
          (cond
            (= target-class java.util.Properties)
            (let [java-props (java.util.Properties.)]
              (doto (java.util.Properties.)
                (.put "byte value" (.getByteVal this))
                (.put "long value" (.getLongVal this))))
            :else this)))
      (with-context ctx
        (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                           :s-vals {:byteVal (byte 1) :intVal (int 3) :longVal (long 4)})
              second-bean (defbean :test-bean-2 boing.test.SimpleClass
                           :s-vals {:props :test-bean-1})]
        (is (= (.toString (create-bean second-bean)) "0:0:0:0:null:0.0:0.0:\\u0000:false:{byte value=1, long value=4}")))))))

