(ns personal-assistant.core
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

; Config is loaded from resources/config.edn at startup.
; aero/read-config merges #env reader literals so any key can be
; overridden via an environment variable without changing the file.
(defn load-config []
  (aero/read-config (io/resource "config.edn")))

(defn greet []
  (println "Hello from Personal Assistant!")
  :ok)

(defn -main [& _args]
  (greet))
