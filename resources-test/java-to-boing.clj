
"This file is an example of a boing definition called from java)"
(use '[boing.bean] '[boing.context] '[boing.resource] '[clojure.stacktrace])

(try
  (do
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
      :class-override higiebus.adaptors.hms.cache.HMSCache :mode :singleton :post #(.createInstance %)))
  (catch Exception e# (print-cause-trace e#) (throw e#)))

(println "Loaded boing context from java")