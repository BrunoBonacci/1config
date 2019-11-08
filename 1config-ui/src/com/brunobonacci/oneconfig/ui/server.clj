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
            [com.brunobonacci.oneconfig.backend :refer :all]
            [com.brunobonacci.oneconfig.backend :as oneconfig]
            [com.brunobonacci.oneconfig.backends :as b]
            [com.brunobonacci.oneconfig.util :as util]
            [clojure.string :as string]
            [where.core :refer [where]]
            [taoensso.sente.server-adapters.http-kit
             :refer (sente-web-server-adapter)]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]]
            [ring.util.response :refer :all])
  (:gen-class))

(def PORT 5300)
(def NREPL-PORT 5301)
(def index-page-path "resources/public/index.html")
(def backend-name
  (util/default-backend-name))
(def repo-base-url "https://api.github.com/repos/BrunoBonacci/1config")
(def repo-tags-url (str repo-base-url "/git/refs"))

;;
;; 1Config backend used by the UI-server
;;
(def backend (b/backend-factory {:type backend-name}))


(defn- normalize
  [params]

  (cond-> params

    (contains? params :change-num)
    (update :change-num #(Long/parseLong %))))


(defn- get-github-data [url]
  (let [{:keys [body]} @(http/get url)]
    (json/parse-string body)))

;;; TODO is it possible to reuse
(defn- oneconfig-version
  []
  (some->
    (io/resource "1config.version")
    slurp
    (string/trim))
  )

(defn- github-data []
  (let [tags (get-github-data repo-tags-url)]
    {:latest  (->
                 (last tags)
                 (get "ref")
                 (string/split #"/")
                 (last))
     :current (oneconfig-version)
     }
    ))

(defn ping-response [req]
  (println (str "pinging back" req) )
  {:status 200})


(defroutes endpoints
  (GET  "/" [] (slurp index-page-path))

  (GET "/configs" {{:keys [change-num] :as params} :params}
       (let [filters (dissoc params :change-num)
             change-num (when change-num (Long/parseLong change-num))
             results (oneconfig/list backend filters)
             ;; filtering further the result
             results (if change-num
                       (filter (where :change-num = change-num) results)
                       results)]
         (response results)))

  (GET "/footer" []
    (response (github-data)))

  (GET "/configs/keys/:key/envs/:env/versions/:version"
       {:keys [params]}

       (let [entry  (if (:change-num params)
                      (oneconfig/load backend (normalize params))
                      (oneconfig/find backend params))]
         (if entry
           (response entry)
           (not-found "Config entry not found"))))

  (POST "/configs" {params :params {referer "referer"} :headers}
    (try
      (oneconfig/save backend (assoc params :encoded true))
      (response {:status "OK" :message "Entry saved."})
      (catch Exception x
        {:status  400
         :body    {:status "ERROR" :message "Unknown error."
                   :cause (.getMessage x)}}
        )))

  (not-found "Not Found")
)


(def handler
  (-> (hdr/site #'endpoints)
      (wrap-resource "public")
      (wrap-defaults
        (->
          site-defaults
          (assoc-in [:security :anti-forgery] false) ;;todo disable Invalid anti-forgery token
          ))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post])
      (wrap-json-body {:keywords? true})
      wrap-json-response))
;
(defn -main [& args]
  (http-kit/run-server handler {:port PORT})
  (println (format "Server started: http://127.0.0.1:%d" PORT))
  (start-server :bind "0.0.0.0" :port NREPL-PORT))

(comment

  ;; start server for repl
  (def s (http-kit/run-server #'handler {:port PORT}))

  ;; stop server
  (s)
  )
