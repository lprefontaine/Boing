(ns boing.context
  (:use [boing.resource]))

(defrecord Context [id aliases beandefs])

(def ^{:private true :dynamic true} *contexts* (atom {:default (Context. :default {} {})}))
(def ^{:dynamic true} *current-context* :default)  ;; This can be rebound on the fly

(defn get-context
  "Returns the given context by its id, throws an exception otherwise"
  [ctx-id]
  (if-let [ctx (ctx-id @*contexts*)]
    ctx
    (throw (Exception. (format "No such bean context %s" ctx-id)))))

(defn context?
  "Returns true or nil if the id matches a context"
  [ctx-id]
  (ctx-id @*contexts*))

(defn add-beandef
  "Add a bean definition to the current context."
  ([beandef]
    (if-let [ctx (*current-context* @*contexts*)]
      (swap! *contexts* #(merge %1 %2)
        {*current-context* (assoc-in ctx [:beandefs (:id beandef)] beandef)})
      (throw (Exception. (format "No such context %s" *current-context*))))))

(defn get-bean-ids
  "Return the ids of the bean definitions in the current context."
  ([]
  (if-let [ctx (get-context *current-context*)]
    (apply vector (sort (keys (:beandefs ctx))))
    (throw (Exception. (format "No such context %s" *current-context*)))))
  ([ctx-id]
    (if-let [ctx (get-context ctx-id)]
      (apply vector (sort (keys (:beandefs ctx))))
      (throw (Exception. (format "No such context %s" *current-context*))))))

(defn add-context
  "Registers a new empty bean context if it does not exists and returns it.
   Otherwise returns the context found."
  ([ctx-id] (add-context ctx-id {}))
  
  ([ctx-id aliases] 
    (if-let [ctx (context? (keyword ctx-id))] ctx
      (do (swap! *contexts* #(merge %1 %2) { (keyword ctx-id) (Context. ctx-id aliases {})})
        (get-context (keyword ctx-id))))))

(defn merge-contexts!
  "Merge bean definitions of two contexts.
   Beans with the same ids will be replaced by the instances in the 'from' context.
   returns nil.
   If one of the context is undefined, throws an exception.
   Be warned, this mutates the 'to' context."
  [from to]
  (let [ctx-from (get-context from)
         ctx-to (get-context to)
         beandefs (merge (:beandefs ctx-to) (:beandefs ctx-from))]
     (swap! *contexts* #(merge %1 %2) { to (merge ctx-to {:beandefs beandefs}) })
     nil))

(defn find-beandef
  "Find a bean definitions either in the current context or in the given context."
  ([id]
    (if-let [ctx (*current-context* @*contexts*)]
      ((keyword id) (:beandefs ctx))
      (throw (Exception. (format "No such bean %s in context %s" id *current-context*)))))
  ([ctx-id id]
    (if-let [ctx ((keyword ctx-id) @*contexts*)]
      ((keyword id) ctx)
      (throw (Exception. (format "No such bean %s in context %s" id ctx-id))))))



    
