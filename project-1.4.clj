(defproject boing "1.4.1"
  :eval-in :subprocess,
  :compile-path "classes",
  :source-paths ("src"),
  :target-path "target",
  :native-path "target/native",
  :test-paths ("test")
  :resource-paths ("dev-resources" "resources"),
  :target-dir "target/",
   
  :java-source-paths ("src"), :javac-target "1.6", :java-fork true, :javac-options {:debug true},
  :jvm-opts ["-XX:+TieredCompilation"],
  :warn-on-reflection true,
  :prep-tasks ["javac" "compile"],
  
  :profiles {:test {:dependencies [[commons-lang/commons-lang "2.2"]]}}
  
  :test-selectors {:default (constantly true)
                   :formatting :formatting
                   :parsing :parsing
                   :validation :validation
                   :all (constantly true)}
  
  :checkout-deps-shares [:source-paths :resource-paths :compile-path],
  :omit-source true, :omit-default-repositories true, :disable-implicit-clean false,
  :clean-non-project-classes true, :jar-exclusions [#"^\."],
  :aot :all, :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA|DSA)$"],

  :group "boing",
  :manifest {"Implementation-Vendor" "SoftAddicts Inc.",
             "Built-By" "build-manager",
             "Specification-Title" "Boing library",
             "Specification-Version" "1.4",
             "Specification-Vendor" "SoftAddicts Inc.",
             "Implementation-Title" "Boing library",
             "Implementation-Version" "1.4.1"},  

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.trace "0.7.3"]])
