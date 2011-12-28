(ns boing.test.test-resource
  (:use [boing.bean] [boing.resource] [clojure.test]))

(deftest test-load-properties []
  (testing
    "Load a property file either as a file or on the classpath"
    (is (= (load-properties "test.properties")
           {:prop4 "4", :prop3 "3", :prop2 "2", :prop1 "1"}))))

(deftest test-input-stream []
  (testing
    "Load a property file using an input stream"
    (is (= (slurp (get-input-stream "test.properties"))
           "prop1=1\r\nprop2=2\r\nprop3=3\r\nprop4=4\r\n"))       
    (is (= (slurp (get-input-stream "file:/home/lprefontaine/workspaces/DSLs/boing/resources-test/test.properties"))
           "prop1=1\r\nprop2=2\r\nprop3=3\r\nprop4=4\r\n"))))