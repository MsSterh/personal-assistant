(ns personal-assistant.repl
  (:require [clojure.string :as str]
            [personal-assistant.config :as config]
            [personal-assistant.context :as context]
            [personal-assistant.llm :as llm]
            [personal-assistant.memory :as memory]
            [personal-assistant.tools.registry :as registry]))

(defn chat-until-text
  "Call the model with the current `history`, and while the assistant replies
  with `tool_calls`, dispatch each tool, append both the assistant message and
  the tool results to `history`, and call again. Returns the final assistant
  message (one with text content and no tool calls). `history` is an atom."
  [client history]
  (loop []
    (let [reply (llm/chat client @history registry/tool-defs)
          calls (:tool_calls reply)]
      (if (seq calls)
        (do
          (swap! history conj reply)
          (doseq [call calls]
            (println (str "  [tool] " (get-in call [:function :name])
                          " " (get-in call [:function :arguments])))
            (swap! history conj (registry/dispatch call)))
          (recur))
        reply))))

(defn run-loop [client system-msg]
  (let [history (atom [system-msg])]
    (loop []
      (print "> ")
      (flush)
      (when-let [line (read-line)]
        (when-not (contains? #{"quit" "exit"} (str/trim line))
          (swap! history conj {:role "user" :content line})
          (let [reply   (chat-until-text client history)
                content (or (:content reply) "")
                facts   (memory/extract-new-facts content)
                display (memory/strip-markers content)]
            (when (seq facts)
              (memory/append-facts-to-daily-note facts (memory/today-str)))
            (swap! history conj (assoc reply :content display))
            (println display))
          (recur))))))

(defn -main [& _args]
  (let [cfg        (config/load-config)
        client     (llm/->HatoClient (:api-url cfg) (:api-key cfg) (:model cfg))
        soul       (context/load-soul)
        mem-ctx    (memory/build-memory-context
                    (memory/load-user-profile "USER.md")
                    (memory/load-memory "MEMORY.md"))
        sys-content (if (str/blank? mem-ctx)
                      (:content soul)
                      (str (:content soul) "\n\n" mem-ctx))
        system-msg (assoc soul :content sys-content)]
    (run-loop client system-msg)))
