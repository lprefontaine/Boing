(ns boing.test.test-translator
  (:use [boing.translator] [boing.resource] [clojure.test] [clojure.pprint]))

(deftest test-simple-spring-setters []
  (testing
    (is (= (translate-spring-to-boing (get-input-stream "simple-spring-setters.xml"))
"(defbean :test-bean-1 \"boing.test.SimpleClass\" :s-vals { :byteVal \n :shortVal \n :intVal \n :longVal \n :stringVal })\n(defbean :test-bean-2 \"boing.test.SimpleClass\" :s-vals { :doubleVal \n :floatVal })\n(defbean :bean-parent \"boing.test.ComplexClass\" :s-vals { :simpleBeanOne :test-bean-1\n\n :simpleBeanTwo :test-bean-2\n})\n" "(defbean :test-bean-1 \"boing.test.SimpleClass\" :s-vals { \n \n \n \n })\n(defbean :test-bean-2 \"boing.test.SimpleClass\" :s-vals { \n })\n(defbean :bean-parent \"boing.test.ComplexClass\" :s-vals { \n })\n"
))))