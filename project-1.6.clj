(defproject boing "1.6.1"
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
  
  :profiles {:test {:dependencies [[commons-lang/commons-lang "2.2"]]}
             :prod {:omit-source true, :aot [#"^higiebus\..*"],
                    :global-vars {*warn-on-reflection* true *assert* false},
                    :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA|DSA)$"]}}
  
  :test-selectors {:default (constantly true)
                   :formatting :formatting
                   :parsing :parsing
                   :validation :validation
                   :all (constantly true)}
  
  :checkout-deps-shares [:source-paths :resource-paths :compile-path],
  :omit-source true, 
  :repositories ^:replace [["release" {:url "http://repo.softaddicts.ca:9191/archiva/repository/internal"}]],
  :disable-implicit-clean false,
  :clean-non-project-classes true, :jar-exclusions [#"^\."],
  :aot :all, :uberjar-exclusions [#"(?i)^META-INF/[^/]*\.(SF|RSA|DSA)$"],

  :group "boing",
  :manifest {"Implementation-Vendor" "SoftAddicts Inc.",
             "Built-By" "build-manager",
             "Specification-Title" "Boing library",
             "Specification-Version" "1.6",
             "Specification-Vendor" "SoftAddicts Inc.",
             "Implementation-Title" "Boing library",
             "Implementation-Version" "1.6.1"},  

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.trace "0.7.8"]])
