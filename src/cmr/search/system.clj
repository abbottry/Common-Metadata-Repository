(ns cmr.search.system
  (:require [cmr.common.lifecycle :as lifecycle]
            [cmr.common.log :refer (debug info warn error)]
            [cmr.system-trace.context :as context]))

;; Design based on http://stuartsierra.com/2013/09/15/lifecycle-composition and related posts

(def
  ^{:doc "Defines the order to start the components."
    :private true}
  component-order [:log :search-index :web])

(defn create-system
  "Returns a new instance of the whole application."
  [log web search-index]
  {:log log
   :search-index search-index
   :web web
   ;; Zipkin doesn't get started or stopped.
   ;; TODO We should handle other zipkin hosts and ports
   :zipkin (context/zipkin-config "Search")})

(defn start
  "Performs side effects to initialize the system, acquire resources,
  and start it running. Returns an updated instance of the system."
  [this]
  (info "System starting")
  (let [started-system (reduce (fn [system component-name]
                                 (update-in system [component-name]
                                            #(lifecycle/start % system)))
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
                                            #(lifecycle/stop % system)))
                               this
                               (reverse component-order))]
    (info "System stopped")
    stopped-system))
