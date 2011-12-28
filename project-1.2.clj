(defproject boing "1.2.2"
  :min-lein-version "1.6.2"
  :disable-implicit-clean false
  :java-source-path "src"
  :java-fork true
  :javac-target "1.5"
  :warn-on-reflection false
  :target-dir "target/"
  :jar-name "boing-1.2.2.jar" 
  :clean-non-project-classes true
  :class-file-whitelist #"boing/"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :manifest {"Built-By" "build-manager"
             "Specification-Title" "Boing library"
             "Specification-Version" "1.2"
             "Specification-Vendor" "SoftAddicts Inc."
             "Implementation-Title" "Boing library"
             "Implementation-Version" 	"1.2.2"
             "Implementation-Vendor" "SoftAddicts Inc."
             }
  :omit-source true
  :aot :all
  :dependencies [[org.clojure/clojure "1.2.1"]])
