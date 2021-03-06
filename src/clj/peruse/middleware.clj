(ns peruse.middleware
  (:require [cognitect.transit :as t]
            [clojure.tools.logging :as log])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (com.cognitect.transit TransitFactory WriteHandler)))

(def custom-writers {clojure.lang.Var
                     (reify WriteHandler
                       (tag [_ _] "var")
                       (rep [_ v] (TransitFactory/taggedValue "var" (str v)))
                       (stringRep [_ _] nil)
                       (getVerboseHandler [_] nil))

                     java.lang.Class
                     (reify WriteHandler
                       (tag [_ _] "class")
                       (rep [_ c] (TransitFactory/taggedValue "class" (str c)))
                       (stringRep [_ _] nil)
                       (getVerboseHandler [_] nil))})

(defn decode-req-body [{:keys [body] :as req}]
  (let [in (ByteArrayInputStream. (.getBytes (slurp body)))
        r (t/reader in :json)]
    (assoc req :body (t/read r))))

(defn encode-resp-body [{:keys [body] :as resp}]
  (log/info resp)
  (log/info "Body:" body)
  (let [out (ByteArrayOutputStream. 4096)
        w (t/writer out :json {:handlers custom-writers})
        _ (t/write w body)
        nbody (.toString out)]
    (log/info "Serial body:" nbody)
    (assoc resp :body nbody)))

(defn wrap-body-transit [handler]
  (fn [request]
    (-> request
        decode-req-body
        handler
        encode-resp-body)))

(defn wrap-body-str-request [handler]
  (fn [request]
    (log/info "Wrap-body str middle")
    (-> request
        (assoc :body (slurp (:body request)))
        handler)))

(defn wrap-body-transit-response [handler]
  (fn [request]
    (log/info "Wrap-resp middle")
    (-> request
        handler
        encode-resp-body)))