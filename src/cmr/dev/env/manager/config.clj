(ns cmr.dev.env.manager.config
  (:require
    [clojure.string :as string]
    [cmr.dev.env.manager.components.core :as components]
    [cmr.dev.env.manager.util :as util]
    [cmr.transmit.config :as transmit]
    [leiningen.core.project :as project]
    [taoensso.timbre :as log]))

(def config-key :dem)
(def default-config
  {config-key {
    :logging {
      :level :info
      :nss '[cmr]}
    :ports {
      :access-control (transmit/access-control-port)
      :bootstrap (transmit/bootstrap-port)
      :cubby (transmit/cubby-port)
      :index-set (transmit/index-set-port)
      :indexer (transmit/indexer-port)
      :ingest (transmit/ingest-port)
      :kms (transmit/kms-port)
      :metadata-db (transmit/metadata-db-port)
      :search (transmit/search-port)
      :urs (transmit/urs-port)
      :virtual-product (transmit/virtual-product-port)}}})

(defn build
  ""
  ([]
    (build nil))
  ([app-key]
    (let [top-level (project/read)]
      (log/debug "top-level keys:" (keys top-level))
      (log/debug "top-level config:" top-level)
      (log/debug "dem config:" (config-key top-level))
      (log/debug "app-level config:" (app-key top-level))
      (util/deep-merge
       default-config
       (util/deep-merge
        {config-key (config-key top-level)}
        (when app-key
         {config-key (get-in top-level [:profiles app-key config-key])}))))))

(defn app-dir
  [system]
  (components/get-config system config-key :app-dir))

(defn logging
  [system]
  (components/get-config system config-key :logging))

(defn log-level
  [system]
  (components/get-config system config-key :logging :level))

(defn log-nss
  [system]
  (components/get-config system config-key :logging :nss))
