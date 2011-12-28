(ns boing.resource
  "This module provides a basic interface to resource loading."
  (:require [clojure.string :as s])
  (:use [clojure.stacktrace] [clojure.java.io])
  (:import [java.util.jar JarFile] [java.net URLDecoder]
           [java.lang ClassLoader] [java.io File] [java.util Properties]))

(defn find-url
  "Return the URL of a given resource."
  [respath]
  (try
    (if (instance? java.net.URL respath) respath
      (if-let [url (ClassLoader/getSystemResource (s/trim respath))] url
        (java.net.URL. respath)))
    (catch Exception e#
      (print-cause-trace e#) (throw (Exception. (format "Cannot find URL for resource path %s: %s" respath (.getMessage e#)))))))

(defn- access-jar
  "Access a jar file from a url and return a JarFile object to access it"
  [url]
  (let [url-path (.getPath url)
        bang-pos (.indexOf url-path "!")
        jar-path (-> (.getPath url) (.substring 5 bang-pos))]
    (JarFile. (URLDecoder/decode jar-path "UTF-8"))))

(defn enum-resources
  "List resources in the given resource folder.
   If resources are in a jar file, a class in the jar file must be provided so
   it can be located on the classpath.
   A regex pattern can be provided as a string to filter the resources by their names."
  ([respath & {:keys [from-class pattern remove-path]}]
    (try
      (if (nil? from-class)
        (if-let [url (find-url respath)]
          (cond (= (.getProtocol url) "file") 
                (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient []) (-> (File. (.toURI url)) (.list))))
                :else (throw (UnsupportedOperationException.
                               (format "Cannot list files for url %s" url))))
          ())
        (if-let [url (find-url (str (s/replace (.getName from-class) "." "/") ".class"))]
          (cond (= (.getProtocol url) "jar")
                (if-let [jar-file (access-jar url)]
                  (if (nil? pattern)
                    (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient [])
                                         (map #(.getName %) (enumeration-seq (.entries jar-file)))))
                    (persistent! (reduce #(if (nil? %2) %1 (conj! %1 %2)) (transient [])
                                         (map (fn [e] (if (.matches (.getName e) pattern) (.getName e))) (enumeration-seq (.entries jar-file))))))))))
    (catch Exception e# (print-cause-trace e#) (throw e#)))))

(defn get-input-stream
  "Get an input stream on a resource.
   if a class is provided, use it to find the resource specifically
   in the class resource container (jar, class folder, ...."
  ([respath & {:keys [from-class]}]
    (try 
      (if-let [resource-url (find-url respath)]
        (cond (nil? from-class)
              (input-stream resource-url)
              :else
              (do respath (.getResourceAsStream from-class respath))))
      (catch Exception e# (print-cause-trace e#) (throw e#)))))

(defn load-properties
  "Load a property resource file as a map."
  ([respath & {:keys [from-class]}]
    (try 
      (if-let [resource-url (find-url respath)]
        (with-open [stream (get-input-stream resource-url :from-class from-class)]
          (let [properties (doto (Properties.) (.load stream))]
            (persistent! (reduce conj! (transient {})  (map (fn [e] { (keyword (key e)) (val e)}) properties)))))        
        {})
      (catch Exception e# (print-cause-trace e#) (throw e#)))))
     
(defn load-text-resource
  "Load a resource file as a string."
  [respath & {:keys [encoding from-class]}]
    (try 
      (if-let [resource-url (find-url respath)]
        (with-open [rdr (reader (get-input-stream resource-url :from-class from-class))]
          (reduce #(str %1 %2) "" (line-seq rdr))))
      (catch Exception e# (print-cause-trace e#) (throw e#))))

(defn load-and-eval
  "Load a clojure resource file and evaluate it."
  ([respath]
    (try
      (load-and-eval respath (str (.getName *ns*)))
      (catch Exception e# (print-cause-trace e#) (throw e#))))
  ([respath namespace-name]
    (let [nsname (cond (instance? clojure.lang.Namespace namespace-name) (str (.getName namespace-name))
                       (instance? String namespace-name) namespace-name
                       :else (throw (Exception. (format "load-and-eval: not a namespace: %s" namespace-name))))
          file-content (load-text-resource respath)
          code (str "(in-ns '" nsname ") " file-content " (in-ns '" (.name *ns*) ") ")]
      (try
        (load-string code)
        (catch Exception e#
          (spit "/tmp/failed-load.clj" code)
          (throw (Exception. (format "Failed loading %s: %s: %s characters" respath (.getMessage e#) (count code)))))))))

 
      
