(ns com.brunobonacci.oneconfig.ui.server
  (:refer-clojure :exclude [find load list])
  (:require [org.httpkit.server :as http-kit]
            [compojure.core :refer [GET POST defroutes routes]]
            [compojure.handler :as hdr]
            [ring.middleware.defaults :refer [wrap-defaults
                                              site-defaults
                                              api-defaults]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-body wrap-json-response]]

            [org.httpkit.client :as http]
            [clojure.java.io :as io]

            [cheshire.core :as json]
            [com.brunobonacci.oneconfig.backend :as cfg1]
            [com.brunobonacci.oneconfig.backends :as b]
            [com.brunobonacci.oneconfig.util :as util]
            [com.brunobonacci.oneconfig.profiles :as prof]
            [clojure.string :as string]
            [where.core :refer [where]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer :all])
  (:gen-class))



(def PORT 5300)
(def index-page-path "/index.html")
(def backend-name (util/default-backend-name))
(def repo-base-url "https://api.github.com/repos/BrunoBonacci/1config")
(def repo-tags-url (str repo-base-url "/releases"))



;;
;; 1Config backend used by the UI-server
;;
(def backend (b/backend-factory {:type backend-name}))



(defn- normalize
  [params]

  (cond-> params

    (contains? params :change-num)
    (update :change-num #(Long/parseLong %))))



(defn- github-latest-release
  "Retrieve 1config latest version from Github repo"
  [url]
  (let [{:keys [body]} @(http/get url)]
    (->> (json/parse-string body true)
       (map :tag_name)
       first)))



;;; TODO is it possible to reuse
(defn- oneconfig-version
  []
  (some->
   (io/resource "1config.version")
   slurp
   (string/trim)))



(defn- releases-info []
  {:latest  (github-latest-release repo-tags-url)
   :current (oneconfig-version)})



(defn ping-response [req]
  (println (str "pinging back" req) )
  {:status 200})



(defroutes endpoints

  (GET  "/" [] (redirect index-page-path (redirect-status-codes :found)))


  (GET "/configs" {{:keys [change-num] :as params} :params}
       (let [filters (dissoc params :change-num)
             change-num (when change-num (Long/parseLong change-num))
             results (cfg1/list backend filters)
             ;; filtering further the result
             results (if change-num
                       (filter (where :change-num = change-num) results)
                       results)]
         (response results)))


  (GET "/info/versions" []
       (response (releases-info)))


  (GET "/preferences" []
       (response (:preferences (prof/user-profiles))))


  (GET "/configs/keys/:key/envs/:env/versions/:version"
       {:keys [params]}

       (let [entry  (if (:change-num params)
                      (cfg1/load backend (normalize params))
                      (cfg1/find backend params))]
         (if entry
           (response entry)
           (not-found "Config entry not found"))))


  (POST "/configs" {params :params {referer "referer"} :headers}
        (try
          (cfg1/save backend (assoc params :encoded true))
          (response {:status "OK" :message "Entry saved."})
          (catch Exception x
            {:status  400
             :body    {:status "ERROR" :message "Unknown error."
                       :cause (.getMessage x)}})))


  (not-found "Not Found"))



(def handler
  (-> (hdr/site #'endpoints)
     (wrap-resource "public")
     (wrap-defaults
      (-> site-defaults
         ;; TODO: disable Invalid anti-forgery token
         (assoc-in [:security :anti-forgery] false)))
     (wrap-cors :access-control-allow-origin [#".*"]
                :access-control-allow-methods [:get :post])
     (wrap-json-body {:keywords? true})
     wrap-json-response))


;; TODO: remove workaround.
;;
;; workaround for AWS Crypto SDK slow first use The SDK does internal
;; initialization the first time is asked to encrypt/decrypt entries
;; which take several seconds.  To work around this issue the UI will
;; request to decrypt one instance before it starts the server thus
;; elimating the wait for the user.
;; This isn't a good/long term approach as the operator might not have
;; access to the keys for the first entry. Therefore a better solution
;; it is required.
;;
(defn warmup-encryption-sdk
  []
  (println "Initialize encryption libs")
  (try
   (some->> (cfg1/list backend {})
      first
      (#(select-keys % [:key :env :version :change-num]))
      (cfg1/load backend))
   (catch Exception _
     ;; purposely ignoring errors
     )))


(defn -main [& args]
  (warmup-encryption-sdk)
  (http-kit/run-server handler {:port PORT})
  (println (format "Server started: http://127.0.0.1:%d" PORT)))



(comment

  ;; start server for repl
  (def s (http-kit/run-server #'handler {:port PORT}))

  ;; stop server
  (s)
  )
