(ns personal-assistant.llm
  (:require [cheshire.core :as json]
            [hato.client :as hato]))

(defprotocol LLMClient
  (chat [client messages]))

(defrecord HatoClient [api-url api-key model]
  LLMClient
  (chat [_ messages]
    (let [resp (hato/post (str api-url "/chat/completions")
                          {:headers {"Authorization" (str "Bearer " api-key)
                                     "Content-Type"  "application/json"}
                           :body    (json/generate-string {:model model :messages messages})
                           :as      :string})
          body (json/parse-string (:body resp) true)]
      (-> body :choices first :message))))
