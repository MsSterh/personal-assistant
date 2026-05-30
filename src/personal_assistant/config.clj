(ns personal-assistant.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn load-config []
  (let [r (io/resource "config.edn")]
    (when-not r
      (throw (ex-info "config.edn not found; copy config.edn.example and fill in your credentials." {})))
    (aero/read-config r)))
