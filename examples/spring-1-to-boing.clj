(ns examples.spring-to-boing-1
  (:use [boing.bean] [boing.context] [boing.resource] [clojure.contrib.def]))

(def *tmo* nil)
(def *username* nil)
(def *password* nil)

(defbean :alerterBean "higiebus.bus.protocol.V2.alerts.Alerter" :s-vals {:producer :alertProducerBean :facility "UVISADAPTER"})

(defbean :connectionFactoryBean "org.apache.activemq.ActiveMQConnectionFactory"
  :s-vals {:brokerURL (fn [] (format "failover:(tcp://brkmaster:61616?connectionTimeout=%s,tcp://brkslave:61616?connectionTimeout=%s)?randomize=false"))})

(defbean :cacheProviderBean "net.sf.ehcache.hibernate.EhCacheProvider")

(defbean :defaultEventProcessorBean "higiebus.adaptors.hms.events.processors.IgnoredEventProcessor" :s-vals {:alerter :alerterBean})

(defbean :hmsInboundEventFactoryBean "higiebus.adaptors.hms.uvis.events.inbound.UvisInboundEventFactory"
  :s-vals {:alerter :alerterBean :componentStatus :componentMonitorBean :hmsCache :hmsCacheBean :hmsParameters :hmsParametersBean})

(defbean :processorContextBean "higiebus.adaptors.hms.uvis.events.processors.ProcessorContext" :s-vals {:busCache :busCacheBean})

;; Define a bunch of similar top level beans, these are at the top of the hierarchy
(let [beans [["AdmitPatientProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.AdmitPatientProcessor"]
             ["DischargePatientProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.DischargePatientProcessor"]
             ["UpdatePatientProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.UpdatePatientInformationProcessor"]
             ["RequestProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.RequestProcessor"]
             ["ObservationProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.ObservationProcessor"]
             ["VisitProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.UpdateVisitsProcessor"]
             ["CensusProcessorBean" "higiebus.adaptors.hms.uvis.events.processors.HospitalCensusProcessor"]]]
  (dorun (map
           (fn [[bname bclass]]
             (defbean bname bclass
               :s-vals { :hmsSessionFactory :hmsSessionFactoryBean :processorContext :processorContextBean
                        :hmsCache :hmsCacheBean :hmsParameters :hmsParametersBean :alerter :alerterBean })) beans)))

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
(defbean :hmsCacheFactoryBean "higiebus.adaptors.hms.cache.HMSCacheFactory")
(defbean :hmsCacheBean "higiebus.adaptors.hms.cache.HMSCacheFactory" :mode :singleton :post (fn [x] (.createInstance x)))

(defn-memo list-mappings
  "Load Hibernate mappings from the corresponding jar file."
  [] (enum-resources "uvis/dao/mappings" higiebus.adaptors.hms.HMSParameters :pattern  "uvis/dao/mappings/.*[.]xml"))


(defbean :hmsSessionFactoryBean "org.springframework.orm.hibernate3.LocalSessionFactoryBean"
  :s-vals {:dataSource :hmsDataSourceBean
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
           :mappingResources (fn [] (list-mappings))})


(defbean :hmsDataSourceBean "higiebus.adaptors.hms.factories.HMSDataSourceFactory"
  :s-vals {:driverClassName "oracle.jdbc.driver.OracleDriver" :url "jdbc:oracle:thin:@10.0.1.54:1521:uvistest"
           :username (fn [] *username*) :password (fn [] *password*)
           :maxWait (long 10000) :testWhileIdle true	:testOnBorrow true
           :validationQuery "select 1 from dual" :maxActive 20 :maxIdle 8 :minIdle 3 :timeBetweenEvictionRunsMillis (long 900000)
           :numTestsPerEvictionRun 50 :minEvictableIdleTimeMillis (long 1800000)
          }
  :post (fn [x] (.createInstance x)))

;; End of definitions --> Spring XML (nicely formatted) equivalent is nearly 2000 lines.
;; The above: less than 75 lines and its dynamic...

;; From now on using the above definitions is quite simple:
(binding [*tmo* 3000
          *username* "testuser"
          *password* "testpassword"]
  (bean-summary)
  (let [processor (create-bean :AdmitPatientProcessorBean)]
    (println "whatever you need to do with the object")))
