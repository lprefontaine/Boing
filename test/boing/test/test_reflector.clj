(ns boing.test.test-reflector
    (:use [boing.core.reflector] [clojure.test] [clojure.contrib.trace]))


(deftest test-find []
  (testing
    "Testing finders and validations"
    (is (= (into (sorted-map)
                 (map (fn [[k v]] { k (.getName v)}) (find-setters boing.test.SimpleClass)))
           {:boolVal "setBoolVal", :byteVal "setByteVal", :charVal "setCharVal", :doubleVal "setDoubleVal",
            :floatVal "setFloatVal", :intVal "setIntVal", :listVal "setListVal", :longVal "setLongVal",
            :mapVal "setMapVal", :objectVal "setObjectVal", :privateParentVal "setPrivateParentVal",
            :props "setProps", :shortVal "setShortVal", :stringVal "setStringVal", :vector "setVector"} ))

    (is (= (into [] (sort (map #(.getName %) (find-methods boing.test.SimpleClass [(int 1)]))))
           ["setIntVal" "setPrivateParentVal"]))
    (is (= (into [] (sort (map #(.getName %) (find-methods boing.test.SimpleClass [Integer] "setPrivateParentVal"))))
           ["setPrivateParentVal"]))
    (is (= (into [] (sort (map #(.getName %) (find-methods boing.test.ComplexClass [] "init"))))
           ["init"]))
    (is (= (into [] (sort (map #(.toString %) (find-constructors boing.test.SimpleClass [Byte Short]))))
           ["public boing.test.SimpleClass(byte,short)"]))))

(deftest test-invoke []
  (testing
    "Testing innvokers"
    (let [ctor (first (find-constructors boing.test.SimpleClass [Byte Short]))
          mth (first (find-methods boing.test.SimpleClass [Integer] "setPrivateParentVal"))]
      (is (= (.toString (invoke-constructor ctor [(byte 1) (short 3)])) "1:3:0:0:null:0.0:0.0:\\u0000:false"))
      (is (thrown? IllegalArgumentException (invoke-constructor ctor [(byte 1) (int 3)])))
      )))


