(ns cmr.indexer.system
  "Defines functions for creating, starting, and stopping the application. Applications are
  represented as a map of components. Design based on
  http://stuartsierra.com/2013/09/15/lifecycle-composition and related posts."
  (:require [cmr.common.lifecycle :as lifecycle]
            [cmr.common.log :as log :refer (debug info warn error)]
            [cmr.system-trace.context :as context]
            [cmr.common.api.web-server :as web]
            [cmr.indexer.data.elasticsearch :as es]
            [cmr.common.cache :as cache]
            [cmr.acl.acl-cache :as ac]
            [cmr.common.jobs :as jobs]
            [cmr.indexer.api.routes :as routes]
            [cmr.transmit.config :as transmit-config]
            [clojure.string :as str]
            [cmr.common.config :as cfg]
            [cmr.elastic-utils.config :as es-config]
            [cmr.acl.core :as acl]))

(def collections-with-separate-indexes
  "Configuration function that will return a list of collections with separate indexes for their
  granule data."
  (cfg/config-value-fn :colls-with-separate-indexes "" #(str/split % #",")))

(def
  ^{:doc "Defines the order to start the components."
    :private true}
  component-order [:log :db :web :scheduler])

(def system-holder
  "Required for jobs"
  (atom nil))

(defn create-system
  "Returns a new instance of the whole application."
  []
  (let [sys {:log (log/create-logger)
             :db (es/create-elasticsearch-store (es-config/elastic-config))
             ;; This is set as a dynamic lookup to enable easy replacement of the value for testing.
             :colls-with-separate-indexes-fn collections-with-separate-indexes
             :web (web/create-web-server (transmit-config/indexer-port) routes/make-api)
             :zipkin (context/zipkin-config "Indexer" false)
             :relative-root-url (transmit-config/indexer-relative-root-url)
             :caches {ac/acl-cache-key (ac/create-acl-cache)
                      cache/general-cache-key (cache/create-cache)
                      acl/token-imp-cache-key (acl/create-token-imp-cache)}
             :scheduler (jobs/create-scheduler
                          `system-holder
                          :db
                          [(ac/refresh-acl-cache-job "indexer-acl-cache-refresh")])}]
    (transmit-config/system-with-connections sys [:metadata-db :index-set :echo-rest])))

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [this]
  (info "System starting")
  (let [started-system (reduce (fn [system component-name]
                                 (update-in system [component-name]
                                            #(when % (lifecycle/start % system))))
                               this
                               component-order)]

    (info "System started")
    started-system))


(defn stop
  "Performs side effects to shut down the system and release its
  resources. Returns an updated instance of the system."
  [this]
  (info "System shutting down")
  (let [stopped-system (reduce (fn [system component-name]
                                 (update-in system [component-name]
                                            #(when % (lifecycle/stop % system))))
                               this
                               (reverse component-order))]
    (info "System stopped")
    stopped-system))
