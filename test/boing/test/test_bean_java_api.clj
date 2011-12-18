(ns boing.test.test-bean-java-api
  (:use [boing.bean] [boing.context] [clojure.test] [clojure.pprint]
        [clojure.contrib.trace])
  (:import [boing Bean] [java.util Arrays]))

(deftest test-create-bean-constructors []
  (testing
    "Testing bean creation from Java API using only constructors: boing.test.SimpleClass"
    (let [bean-1 (defbean :test-bean-1 boing.test.SimpleClass)
          bean-2 (defbean :test-bean-2 boing.test.SimpleClass :c-args [(byte 1)])
          bean-3 (defbean :test-bean-3 boing.test.SimpleClass
                   :c-args [(byte 1) (short 2) (int 3) (long 4)])
          bean-4  (defbean :test-bean-4 boing.test.SimpleClass
                    :c-args [(byte 1) (short 2) (int 3) (long 4) "Test String"])]

      (is (= (.toString (Bean/createBean "test-bean-1")) "0:0:0:0:null:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-bean-2")) "1:0:0:0:null:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-bean-3")) "1:2:3:4:null:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-bean-4")) "1:2:3:4:Test String:0.0:0.0:\\u0000:false" )))))

(deftest test-create-bean-w-setters []
  (testing
    "Testing bean creation using an empty constructor and setters: boing.test.SimpleClass"
    (let [bean-1 (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})
          bean-2 (defbean :test-bean-2 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})]
      (is (= (.toString (Bean/createBean "test-bean-1"))
             "1:2:3:4:This is a test:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-bean-2"))
             "0:0:0:0:null:2.3:3.4:H:true")))))

(deftest test-create-complex []
    (testing
      "Testing bean creation referring to other beans"
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})
            complex-bean (create-bean (defbean :test-complex-bean-1 boing.test.ComplexClass
                   :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean}))]
        (is (= (.toString (Bean/createBean "test-complex-bean-1"))
               "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false"))))
    (testing
      "Testing beans creation referring to other anonymous beans"
      (let [first-bean (defabean boing.test.SimpleClass
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defabean boing.test.SimpleClass
                          :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})
            complex-bean (defbean :test-complex-bean-2 boing.test.ComplexClass
                                :s-vals {:simpleBeanOne first-bean
                                :simpleBeanTwo second-bean})]
        (is (= (.toString
                 (Bean/createBean "test-complex-bean-2")) "0:0:0:0:null:2.3:3.4:H:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false")))))

(deftest test-singleton-setters []
    (testing
      "Testing singleton creation using setters"
      (let [singleton (defbean :test-bean-singleton-1 boing.test.SimpleClass :mode :singleton
                         :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            alien-singleton
            (with-context :my-ctx
              (defbean :test-bean-singleton-1 boing.test.SimpleClass :mode :singleton
                :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true}))]
        (is (= (.hashCode (Bean/createBean "test-bean-singleton-1")) (.hashCode (Bean/createBean "test-bean-singleton-1"))))
        (is (= (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1")) (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1"))))
        (is (not (= (.hashCode (Bean/createBean "test-bean-singleton-1")) (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1"))))))))

(deftest test-singleton-constructor []
    (testing
      "Testing singleton creation using constructor"
      (let [singleton (defbean :test-bean-singleton-1 boing.test.SimpleClass :mode :singleton
                         :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true])
            alien-singleton
            (with-context :my-ctx
              (defbean :test-bean-singleton-1 boing.test.SimpleClass :mode :singleton
                :c-args [(byte 1) (short 2) (int 3) (long 4) "Test string" (float 1.1) (double 1.2) \H true]))]
        (is (= (.hashCode (Bean/createBean "test-bean-singleton-1")) (.hashCode (Bean/createBean "test-bean-singleton-1"))))
        (is (= (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1")) (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1"))))
        (is (not (= (.hashCode (Bean/createBean "test-bean-singleton-1")) (.hashCode (Bean/createBeanFromContext "my-ctx" "test-bean-singleton-1"))))))))


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
              global-overrides (Arrays/asList (to-array ["s3" (Arrays/asList (to-array [ "charVal" \Y]))]))]
          (is (= (apply str (Bean/createSingletons))
 "[\"s3\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=6, a=5}>][\"s2\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=4, a=3}>][\"s1\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}>]"))
          (is (= (apply str (Bean/createSingletons nil))
 "[\"s3\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=6, a=5}>][\"s2\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=4, a=3}>][\"s1\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}>]"))
          (is (= (apply str (Bean/createSingletons global-overrides))
 "[\"s3\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=6, a=5}>][\"s2\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=4, a=3}>][\"s1\" #<SimpleClass 0:0:0:0:null:0.0:0.0:\\u0000:false:{b=2, a=1}>]")))))))

(deftest test-overrides []
  (testing
    "Test instantiation using value overrides"
    (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                       :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
          second-bean (defbean :test-bean-2 boing.test.SimpleClass
                        :s-vals {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})
          complex-bean (defbean :test-complex-bean-2 boing.test.ComplexClass
                         :s-vals {:simpleBeanOne first-bean
                                  :simpleBeanTwo second-bean})
          local-overrides (Arrays/asList (to-array ["byteVal" (byte 3)]))
          global-overrides (Arrays/asList (to-array ["test-bean-1" (Arrays/asList (to-array [ "charVal" \Y]))]))]
      (is (= (.toString (Bean/createBean "test-bean-2")) "1:2:3:4:This is a test:0.0:0.0:\\u0000:false" ))
      (is (= (.toString (Bean/createBean "test-bean-2" local-overrides)) "3:2:3:4:This is a test:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-bean-2" nil global-overrides)) "1:2:3:4:This is a test:0.0:0.0:\\u0000:false"))
      (is (= (.toString (Bean/createBean "test-complex-bean-2" nil global-overrides)) "0:0:0:0:null:2.3:3.4:Y:true:1:2:3:4:This is a test:0.0:0.0:\\u0000:false" )))))
