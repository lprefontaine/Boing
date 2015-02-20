(ns boing.test.test-context
    (:use [boing.bean] [boing.context] [clojure.test]))

(deftest test-singleton []
    (testing
      "Instantiate singleton"
      (with-context :my-ctx
	      (let [singleton (defbean :test-bean-1 boing.test.SimpleClass :mode :singleton
	                         :c-args [(jbyte 1) (jshort 2) (jint 3) (jlong 4) "Test string" (jfloat 1.1) (jdouble 1.2) \H true])
	            alien-singleton 
	            (with-context :my-ctx
	              (defbean :test-bean-1 boing.test.SimpleClass :mode :singleton
	                :s-vals {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true}))]
	        (is (= (.hashCode (create-bean singleton)) (.hashCode (create-bean singleton))))
	        (with-context :my-ctx
	          (is (= (.hashCode (create-bean alien-singleton)) (.hashCode (create-bean alien-singleton)))))
	        ))))

(deftest test-default-context []
  (testing "Testing default bean context"
    (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
          second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :properties {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]      
      (is (= (.hashCode first-bean) (.hashCode (find-beandef :test-bean-1))))
      (is (= first-bean (find-beandef :test-bean-1)))
      (is (= (get-bean-ids) [:test-bean-1 :test-bean-2])))))

(deftest test-arbitrary-context []
  (testing "Testing default bean context"
    (with-context :alternate
      (let [first-bean (defbean :test-bean-1 boing.test.SimpleClass
                         :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
            second-bean (defbean :test-bean-2 boing.test.SimpleClass
                          :properties {:byteVal (byte 1) :shortVal (short 2) :intVal (int 3) :longVal (long 4) :stringVal "This is a test"})]      
        (is (= (.hashCode first-bean) (.hashCode (find-beandef :test-bean-1))))
        (is (= first-bean (find-beandef :test-bean-1)))
        (is (= (get-bean-ids) [:test-bean-1 :test-bean-2])))
        (is (= *current-context* :alternate)))))

(deftest test-merge-context []
  (testing "Testing context merge"
    (with-context :alternate-a
      (defbean :test-bean-1 boing.test.SimpleClass
               :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
      (defbean :test-bean-2 boing.test.SimpleClass
        :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true}))
    (with-context :alternate-b
      (defbean :test-bean-3 boing.test.SimpleClass
               :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true})
      (defbean :test-bean-4 boing.test.SimpleClass
        :properties {:floatVal (float 2.3) :doubleVal (double 3.4) :charVal \H :boolVal true}))
    (merge-contexts! :alternate-a :alternate-b)
    (is (= (get-bean-ids :alternate-b) [:test-bean-1 :test-bean-2 :test-bean-3 :test-bean-4]))
    (is (= (get-bean-ids :alternate-a) [:test-bean-1 :test-bean-2]))))