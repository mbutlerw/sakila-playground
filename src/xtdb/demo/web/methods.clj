(ns xtdb.demo.web.methods
  (:require
   [clojure.string :as str]
   [juxt.pick.ring :refer [pick]]
   [xtdb.demo.web.conditional :refer [evaluate-preconditions!]]))

(defn allowed-methods [resource request]
  (let [methods (set (keys (:methods resource {"GET" {}})))]
    (keep
     (fn [x] (case x
               "GET" (when (contains? methods "GET") x)
               "HEAD" (when (contains? methods "GET") x)
               "POST" (when (contains? methods "POST") x)
               "PUT" (when (contains? methods "PUT") x)
               "DELETE" (when (contains? methods "DELETE") x)
               "OPTIONS" (when true x)))
     ["GET" "HEAD" "POST" "PUT" "DELETE" "OPTIONS"])))

(defn select-representation [representations request]
  (when request
    (:juxt.pick/representation (pick request representations))))

(defn request-applied-metadata [representation request]
  (or
   (some-> representation meta #_(update-vals (fn [v] (if (fn? v) (v request) v))))
   {}))

(defn GET [resource request]
  (if-let [f (get-in resource [:methods "GET" :handler])]
    (let [response (f resource request)]
      (merge {:ring.response/status 200} response))
    ;; No explicit handler, so check for representations
    (let [representation
          (-> resource :representations (select-representation request))]
      (evaluate-preconditions! (meta representation) request)
      (if-not representation
        (let [response (get-in resource [:responses 404])]
          (cond-> {:ring.response/status 404}
            response (merge (response request))))
        (let [representation-metadata (request-applied-metadata representation request)
              {:ring.response/keys [status headers body]} (representation request)]
          {:ring.response/status (or status 200)
           :ring.response/headers (into representation-metadata headers)
           :ring.response/body body})
        ))))

(defn HEAD [resource request]
  (let [representation
        (-> resource :representations (select-representation request))]
    {:ring.response/status 200
     :ring.response/headers (request-applied-metadata representation request)}))

(defn PUT [resource request]
  (throw (ex-info "TODO" {})))

(defn POST [resource request]
  (if-let [f (get-in resource [:methods "POST" :handler])]
    ;; Lazily receive representation from request
    (let [response (f resource request)]
      (merge {:ring.response/status 201} response))
    {:ring.response/status 405}))

(defn DELETE [resource request]
  (if-let [f (get-in resource [:methods "DELETE" :handler])]
    (let [response (f resource request)]
      (merge {:ring.response/status 204} response))
    {:ring.response/status 405}))

(defn OPTIONS [resource request]
  (let [methods (allowed-methods resource request)]
    {:ring.response/status 200
     :ring.response/headers {"allow" (str/join ", " methods)}}))
