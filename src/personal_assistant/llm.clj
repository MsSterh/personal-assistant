(ns personal-assistant.llm
  (:require [cheshire.core :as json]
            [hato.client :as hato]))

(defprotocol LLMClient
  (chat [client messages] [client messages tools]
    "Send `messages` to the model. With `tools` (a vector of OpenAI-format
     function schemas), the returned assistant message may carry `:tool_calls`."))

(defrecord HatoClient [api-url api-key model]
  LLMClient
  (chat [this messages] (chat this messages nil))
  (chat [_ messages tools]
    (let [payload (cond-> {:model model :messages messages}
                    (seq tools) (assoc :tools tools))
          resp (hato/post (str api-url "/chat/completions")
                          {:headers {"Authorization" (str "Bearer " api-key)
                                     "Content-Type"  "application/json"}
                           :body    (json/generate-string payload)
                           :as      :string})
          body (json/parse-string (:body resp) true)]
      (-> body :choices first :message))))
