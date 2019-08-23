(ns com.brunobonacci.oneconfig.ui.server
  (:use ring.util.response)
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
            [taoensso.sente.server-adapters.http-kit
             :refer (sente-web-server-adapter)]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [clojure.tools.nrepl.server :refer [start-server stop-server]])
  (:gen-class))

(def PORT 5300)
(def NREPL-PORT 5301)
(def backend-name
  (util/default-backend-name))
(def server-state (atom {:likes 0}))

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

(def handler
  (-> (hdr/site endpoints)
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