(defproject boing "1.3.1"
  :min-lein-version "1.7.1"
  :disable-implicit-clean false
  :java-source-path "src"
  :java-fork true
  :javac-target "1.5"
  :warn-on-reflection false
  :target-dir "target/"
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
             "Implementation-Version" 	"1.3.0"
             "Implementation-Vendor" "SoftAddicts Inc."
             }
  :omit-source true
  :aot :all
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.trace "0.7.3"]])
