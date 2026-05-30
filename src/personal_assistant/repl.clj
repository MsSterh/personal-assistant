(ns personal-assistant.repl
  (:require [clojure.string :as str]
            [personal-assistant.config :as config]
            [personal-assistant.context :as context]
            [personal-assistant.llm :as llm]))

(defn run-loop [client system-msg]
  (let [history (atom [system-msg])]
    (loop []
      (print "> ")
      (flush)
      (when-let [line (read-line)]
        (when-not (contains? #{"quit" "exit"} (str/trim line))
          (swap! history conj {:role "user" :content line})
          (let [reply (llm/chat client @history)]
            (swap! history conj reply)
            (println (:content reply)))
          (recur))))))

(defn -main [& _args]
  (let [cfg    (config/load-config)
        client (llm/->HatoClient (:api-url cfg) (:api-key cfg) (:model cfg))
        soul   (context/load-soul)]
    (run-loop client soul)))
