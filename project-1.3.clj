(defproject boing "1.3.0"
  :min-lein-version "1.6.2"
  :disable-implicit-clean false
  :java-source-path "src"
  :java-fork true
  :javac-target "1.5"
  :warn-on-reflection false
  :target-dir "target/"
    :jar-name "boing-1.3.0.jar" 
  :clean-non-project-classes true
  :manifest {"Built-By" "build-manager"
             "Specification-Title" "Boing library"
             "Specification-Version" "1.2"
             "Specification-Vendor" "SoftAddicts Inc."
             "Implementation-Title" "Boing library"
             "Implementation-Version" 	"1.3.0"
             "Implementation-Vendor" "SoftAddicts Inc."
             }
  :omit-source false
  :aot :all
  :dependencies [[org.clojure/clojure "1.3.0"]])
