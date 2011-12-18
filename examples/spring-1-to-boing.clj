(ns examples.spring-to-boing-1
  "This file is an example of recoding the spring-1.xml file using boing.
   Some of the beans are in fact using some Spring classes but they are not relevant
   to Spring beans. It happens that the example used had some wired ORM related
   Spring objects used.

   Some definitionss are factory classes. Contrary to Spring, they can be used
   as property values. The trick here is the post function which
   returns directly whatever object we expect from the factory.

   The main purpose was to demonstrate that boing is much more shorter than its Spring
   XML counterpart. (109 lines versus > 1900 lines) and much more dynamic.

   In this example, we are using the default context (:default)"

  (:use [boing.bean] [boing.context] [boing.resource]
        [clojure.contrib.def]))

(set! *warn-on-reflection* true)

;; Global variables to rebind when creating beans
(def *username* nil)
(def *password* nil)

(time (do
	;; Bean definitions
	(defbean :alerterBean "higiebus.bus.protocol.V2.alerts.Alerter" :s-vals {:producer :alertProducerBean :facility "UVISADAPTER"})
	(defbean :connectionFactoryBean "org.apache.activemq.ActiveMQConnectionFactory"
	  :s-vals {:brokerURL
	           #(format "failover:(tcp://brkmaster:61616?connectionTimeout=%s,tcp://brkslave:61616?connectionTimeout=%s)?randomize=false"
	                    (override :global :tmo) (override :global :tmo))}) ;; Get overriden values at bean creation time
	(defbean :cacheProviderBean "net.sf.ehcache.hibernate.EhCacheProvider")
	(defbean :defaultEventProcessorBean "higiebus.adaptors.hms.events.processors.IgnoredEventProcessor" :s-vals {:alerter :alerterBean})
	(defbean :hmsInboundEventFactoryBean "higiebus.adaptors.hms.uvis.events.inbound.UvisInboundEventFactory"
	  :s-vals {:alerter :alerterBean :componentStatus :componentMonitorBean :hmsCache :hmsCacheBean :hmsParameters :hmsParametersBean})
	(defbean :processorContextBean "higiebus.adaptors.hms.uvis.events.processors.ProcessorContext"
	  :s-vals {:busCache (defbean :busCacheBean "higiebus.bus.cache.BusCacheFactory"
	                       :post #(.createInstance %) :class-override higiebus.bus.cache.BusCache)})

	(defbean :alertProducerBean "higiebus.tools.jms.Producer"
	  :s-vals {:subject "HIGIEBUS.ALERT" :connectionFactory :connectionFactoryBean :transacted false :name "HIGIEBUSCore" :ackMode "AUTO_ACKNOWLEDGE"})

	(defbean :hmsParametersBean "higiebus.adaptors.hms.uvis.HMSUvisParameters" :mode :singleton
	  :s-vals {:senderApplicationName "HIGIEBUS" :senderApplicationInstance "PROTOTYPE"
	           :radiologyAreas {28 "ECHO" 21 "RX-GA" 22 "RX-PA" 27 "TORE"}
	           :radiologyAnswersExamDescriptions
	           { 897 "Examen spécifiques" 878 "DONNÉES PERTINENTES ET/OU SIGNES CLINIQUES"
	            901 "DONNÉES PERTINENTES ET/OU SIGNES CLINIQUES" 922 "DONNÉES PERTINENTES ET/OU SIGNES CLINIQUES"
	            1809 "DONNÉES PERTINENTES ET/OU SIGNES CLINIQUES"}
	           :requestUpdateStatusFilter
	           { "NEW" false "REQUESTED" true "ACCEPTED" true "MODIFIED" true "SCHEDULED" true "PRELIMINARY" true "CANCELLED" false
	            "REJECTED" true "COMPLETE" true "AMENDED" true "PERFORMED" true "ADDENDED" true}})

	(defbean :HMSPatientDaoBean "higiebus.adaptors.hms.uvis.dao.UvisPatientDao")
	(defbean :HMSClientDaoBean "higiebus.adaptors.hms.uvis.dao.UvisClientDao")
	(defbean :HMSOrderDaoBean "higiebus.adaptors.hms.uvis.dao.UvisOrderDao")
	(defbean :HMSEmployeeDaoBean "higiebus.adaptors.hms.uvis.dao.UvisEmployeeDao")
	(defbean :HMSRequestDaoBean "higiebus.adaptors.hms.uvis.dao.UvisRequestDao")
	(defbean :HMSResultDaoBean "higiebus.adaptors.hms.uvis.dao.UvisResultDao")
	(defbean :HMSEpisodeDaoBean "higiebus.adaptors.hms.uvis.dao.UvisEpisodeDao")
	(defbean :HMSRequestAnswerDaoBean "higiebus.adaptors.hms.uvis.dao.UvisRequestAnswerDao")
	(defbean :HMSDiagnosisDaoBean "higiebus.adaptors.hms.uvis.dao.UvisDiagnosisDao")
	(defbean :HMSHospitalCensusDaoBean "higiebus.adaptors.hms.uvis.dao.UvisHospitalCensusDao")
	(defbean :hmsCacheBean "higiebus.adaptors.hms.cache.HMSCacheFactory"
	  :class-override higiebus.adaptors.hms.cache.HMSCache :mode :singleton :post #(.createInstance %))

	(defn-memo list-mappings
	  "Load Hibernate mappings from the corresponding jar file."
	  [] (enum-resources "uvis/dao/mappings" :from-class higiebus.adaptors.hms.HMSParameters :pattern  "uvis/dao/mappings/.*[.]xml"))

	(defbean :hmsSessionFactoryBean "org.springframework.orm.hibernate3.LocalSessionFactoryBean"
	  :mode :singleton
	  :s-vals {:dataSource
	           (defbean :hmsDataSourceBean "higiebus.adaptors.hms.factories.HMSDataSourceFactory"
	             :s-vals {:driverClassName "oracle.jdbc.driver.OracleDriver" :url "jdbc:oracle:thin:@10.0.1.54:1521:uvistest"
	                      :username (fn [] *username*) :password nil ;; Get username via thread bindings, password with property override
	                      :maxWait (long 10000) :testWhileIdle true	:testOnBorrow true
	                      :validationQuery "select 1 from dual" :maxActive 20 :maxIdle 8 :minIdle 3 :timeBetweenEvictionRunsMillis (long 900000)
	                      :numTestsPerEvictionRun 50 :minEvictableIdleTimeMillis (long 1800000)
	                      }
	             :class-override javax.sql.DataSource
	             :post #(.createInstance %))
	           :cacheProvider :cacheProviderBean
	           :hibernateProperties {:hibernate.dialect "org.hibernate.dialect.HSQLDialect"
	                                 :hibernate.generate_statistics false
	                                 :hibernate.transaction.flush_before_completion true
	                                 :hibernate.transaction.auto_close_session true
	                                 :hibernate.show_sql false
	                                 :hibernate.c3p0.acquire_increment 3
	                                 :hibernate.c3p0.idle_test_period 200
	                                 :hibernate.c3p0.timeout 200
	                                 :hibernate.c3p0.max_size 300
	                                 :hibernate.c3p0.min_size 3
	                                 :hibernate.c3p0.max_statements 0
	                                 :hibernate.c3p0.preferredTestQuery "select 1;"
	                                 :hibernate.cache.use_second_level_cache true
	                                 :hibernate.cache.use_query_cache true
	                                 :net.sf.ehcache.configurationResourceName "conf/ehcache.xml" }
	           :mappingResources #(list-mappings)}
	  :class-override org.hibernate.SessionFactory
	  :post      ;; This Spring object implements the Factory interface. getObject returns an new object from the factory
	  #(.getObject %))

	;; Define a bunch of similar top level beans, these are at the top of the hierarchy
	(let [beans ["AdmitPatientProcessor" ["UpdatePatientProcessor" "UpdatePatientInformationProcessor"]
	             "RequestProcessor" "ObservationProcessor" "UpdateVisitsProcessor" ["CensusProcessorBean" "HospitalCensusProcessor"]]]
	  (dorun (map (fn [bname]
	                (let [bean-name (if (vector? bname) (get bname 0) bname) class-name (if (vector? bname) (get bname 1) bname)]
	                  (defbean (str bean-name "Bean") (str "higiebus.adaptors.hms.uvis.events.processors." class-name)
	                    :s-vals { :hmsSessionFactory :hmsSessionFactoryBean :processorContext :processorContextBean
	                             :hmsCache :hmsCacheBean :hmsParameters :hmsParametersBean :alerter :alerterBean }))) beans)))
))
;; End of definitions

;; From now on using the above definitions is quite simple:
(binding [*username* "testuser"
          *password* "testpassword"]
  (System/setProperty "higiebus.adaptors.hms.datasourceProperties","examples/fakedproperties.properties")
  (time (let [processor (create-bean :AdmitPatientProcessorBean {} ;; No override for values in this bean
                                     ;; Some globally available overrides. :global is not a bean definition,
                                     ;; :hmsDataSourceBean is.
                                     {:hmsDataSourceBean {:password "testpassword"} :global {:tmo 300}})]
          (println "whatever you need to do with the object")))
  (time (let [processor (create-bean :RequestProcessorBean {} ;; No override for values in this bean
                                     ;; Some globally available overrides. :global is not a bean definition,
                                     ;; :hmsDataSourceBean is.
                                     {:hmsDataSourceBean {:password "testpassword"} :global {:tmo 300}})]
          (println "whatever you need to do with the object")))
  )
