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

            [clojure.core.async :as a :refer [go-loop <!]]

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
(def backend-name
  (util/default-backend-name))
(def server-state (atom {:likes 0}))

;;
;; 1Config backend used by the UI-server
;;
(def backend (b/backend-factory {:type backend-name}))


(defn- normalize
  [params]

  (cond-> params

    (contains? params :change-num)
    (update :change-num #(Long/parseLong %))))


(defn ping-response [req]
  (println (str "pinging back" req) )
  {:status 200})


(defroutes endpoints
           (POST "/oneconfig-add-entry"  {:keys [headers params body] :as request}
             (let [value-file (if (nil? (get-in params [:current-files :tempfile]))
                                ""
                                (slurp (get-in params [:current-files :tempfile])))
                   name            (get params :key)
                   env             (get params  :env)
                   version         (get params  :version)
                   content-type    (get params  :content-type)
                   value-textarea  (get params  :value)
                   value           (if (string/blank? value-file) value-textarea value-file)]
               (oneconfig/save (b/backend-factory {:type backend-name})
                               {:key name :env env :version version :content-type content-type  :value value})
               (response (oneconfig/list (b/backend-factory {:type backend-name}) {}))
               ))

           (GET "/oneconfig-list" []
             (response (oneconfig/list (b/backend-factory {:type backend-name}) {})))

           (GET "/oneconfig-get-item"{{:keys [key env version change-num content-type]}  :params}
             (response
               (->
                 (oneconfig/find (b/backend-factory {:type backend-name}) {:key key :env env :version version})
                 :encoded-value)))

           (GET "/oneconfig-apply-filter" {{:keys [key env version]}  :params}
             (response
               (oneconfig/list (b/backend-factory {:type backend-name}) {:key key :env env :version version})))

           (POST "/oneconfig-add-new-value" request
             (ping-response request))

           (not-found "Not Found"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;                                                                            ;;
;;                     ----==| N E W   L A Y E R |==----                      ;;
;;                                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

  (GET "/configs" {{:keys [change-num] :as params} :params}
       (let [filters (dissoc params :change-num)
             change-num (when change-num (Long/parseLong change-num))
             results (oneconfig/list backend filters)
             ;; filtering further the result
             results (if change-num
                       (filter (where :change-num = change-num) results)
                       results)]
         (response results)))


  (GET "/configs/keys/:key/envs/:env/versions/:version"
       {:keys [params]}

       (let [entry  (if (:change-num params)
                      (oneconfig/load backend (normalize params))
                      (oneconfig/find backend params))]

         (if entry
           (response entry)
           (not-found "Config entry not found"))))



  (POST "/configs" {:keys [body]}
        (oneconfig/save backend body)
        (response "ok"))


  (not-found "Not Found"))


(def handler
  (-> (hdr/site #'endpoints)
      (wrap-resource "public")
      (wrap-defaults
        (->
          site-defaults
          (assoc-in [:security :anti-forgery] false) ;;tODO disable Invalid anti-forgery token
          ))
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post])
      (wrap-json-body {:keywords? true})
      wrap-json-response))
;
(defn -main [& args]
  (http-kit/run-server handler {:port PORT})
  (println (str "Server started: http://127.0.0.1:" PORT "/index.html"))
  (start-server :bind "0.0.0.0" :port NREPL-PORT)
  (println (str "nRepl server started: `lein repl :connect " NREPL-PORT "`")))



(comment

  (System/setProperty "aws.accessKeyId" "")
  (System/setProperty "aws.secretKey"   "")
  (System/setProperty "aws.region" "")


  ;; start server for repl
  (def s (http-kit/run-server #'handler {:port PORT}))

  ;; stop server
  (s)
  )
