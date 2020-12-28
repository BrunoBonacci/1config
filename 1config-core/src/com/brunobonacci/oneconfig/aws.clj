(ns ^{:author "Bruno Bonacci (@BrunoBonacci)" :no-doc true}
 com.brunobonacci.oneconfig.aws
  (:require
   [com.brunobonacci.oneconfig.util :refer [lazy-mapcat]]
   [cognitect.aws.client.api  :as aws]
   [cognitect.aws.credentials :as credentials]))



(defn invoke
  "Invokes an AWS request and returns the result.
  Errors are contained within the result"
  [client operation request]
  (aws/invoke client {:op      operation
                      :request request}))



(defn anomaly?
  "If an anomaly category is detected there has been an error."
  [response]
  (:cognitect.anomalies/category response))



(defn invoke!
  "Wraps `invoke` and will throw any errors as exceptions"
  [client operation request]
  (let [resp (invoke client operation request)]
    (when (anomaly? resp)
      (throw
        (ex-info
          (str "ERROR - executing " operation " - "
            (or
              (:cognitect.anomalies/message resp)
              (-> resp :Error :Message)
              (resp :Message)
              (resp :message)))
          resp)))
    resp))



(defn- create-client
  "Creates an AWS client for the specified api."
  [{:keys [api region auth endpoint-override]}]
  (aws/client
    (-> {:api api}
      (merge (when region {:region region}))
      (merge (when auth {:credentials-provider auth}))
      (merge (when endpoint-override
               {:endpoint-override endpoint-override})))))



(def make-client
  "Creates a client for the specified api with the given configuration"
  (memoize
    (fn [{:keys [region endpoint-override] :as cfg} api]
      (create-client
        {:api               api
         :region            region
         :auth              nil ;; default chain
         :endpoint-override (or (:all endpoint-override)
                              (api endpoint-override))}))))



(defn help
  "Will return options of what the aws api can do.

  `[client]` - Will list possible operations
  `[client op]` - Will list detail about the operation"
  ([client]
   (->> (aws/ops client)
     (map first)
     sort))
  ([client op]
   (aws/doc client op)))



(defn lazy-paginated-query
  "It creates a generic wrapper for a AWS paginated query"
  [query-fn last-token-name next-token-name result-fn]
  (fn lazy-query
    ([client query]
     (lazy-mapcat result-fn (lazy-query client query nil)))
    ([client query page-token]
     (let [result (query-fn
                    client
                    (cond-> query
                      page-token (assoc next-token-name page-token)))]
       (lazy-seq
         (if-let [next-page (get result last-token-name)]
           (cons result
             (lazy-query client query next-page))
           [result]))))))



(defn default-cfg
  "returns aws default config. TODO: future use"
  []
  {})



(defn get-caller-identity
  []
  (invoke! (make-client (default-cfg) :sts) :GetCallerIdentity {}))
