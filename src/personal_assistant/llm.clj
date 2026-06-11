(ns personal-assistant.llm
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [hato.client :as hato]))

(defprotocol LLMClient
  (chat [client messages] [client messages tools]
    "Send `messages` to the model. With `tools` (a vector of OpenAI-format
     function schemas), the returned assistant message may carry `:tool_calls`."))

;; ---------------------------------------------------------------------------
;; OpenAI (chat completions) — the internal message/tool format is OpenAI's, so
;; this client passes everything straight through.
;; ---------------------------------------------------------------------------

(defrecord OpenAIClient [api-url api-key model]
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

;; ---------------------------------------------------------------------------
;; Anthropic (messages API). The rest of the app speaks the OpenAI shape, so
;; this client translates at both edges: OpenAI-format messages/tools -> the
;; Anthropic request, and the Anthropic response -> an OpenAI-format reply.
;; ---------------------------------------------------------------------------

(defn- ->anthropic-tools
  "OpenAI function schema -> Anthropic tool: {:name :description :input_schema}."
  [tools]
  (mapv (fn [{:keys [function]}]
          {:name        (:name function)
           :description (:description function)
           :input_schema (:parameters function)})
        tools))

(defn- ->anthropic-messages
  "Translate the OpenAI-format history into Anthropic messages, pulling out the
  system prompt. Returns {:system <string> :messages [..]}. Assistant tool_calls
  become `tool_use` blocks; `{:role \"tool\"}` results become a user turn with a
  `tool_result` block (Anthropic combines consecutive user turns)."
  [messages]
  (let [system (->> messages
                    (filter #(= "system" (:role %)))
                    (map :content)
                    (str/join "\n"))
        msgs (reduce
              (fn [acc {:keys [role content tool_calls tool_call_id]}]
                (case role
                  "system" acc
                  "user" (conj acc {:role "user" :content content})
                  "tool" (conj acc {:role "user"
                                    :content [{:type "tool_result"
                                               :tool_use_id tool_call_id
                                               :content (str content)}]})
                  "assistant"
                  (let [text-block (when (seq content)
                                     [{:type "text" :text content}])
                        tool-blocks (for [{:keys [id function]} tool_calls]
                                      {:type  "tool_use"
                                       :id    id
                                       :name  (:name function)
                                       :input (let [a (:arguments function)]
                                                (if (string? a)
                                                  (json/parse-string a true)
                                                  a))})
                        blocks (vec (concat text-block tool-blocks))]
                    (conj acc {:role "assistant" :content blocks}))
                  acc))
              []
              messages)]
    {:system system :messages msgs}))

(defn- anthropic->reply
  "Anthropic response body -> OpenAI-format assistant message. Text blocks are
  concatenated into :content; tool_use blocks become :tool_calls whose
  :arguments is a JSON string (matching what the rest of the app expects)."
  [body]
  (let [blocks (:content body)
        text (->> blocks
                  (filter #(= "text" (:type %)))
                  (map :text)
                  (str/join))
        tool-calls (->> blocks
                        (filter #(= "tool_use" (:type %)))
                        (mapv (fn [{:keys [id name input]}]
                                {:id   id
                                 :type "function"
                                 :function {:name name
                                            :arguments (json/generate-string input)}})))]
    (cond-> {:role "assistant" :content text}
      (seq tool-calls) (assoc :tool_calls tool-calls))))

(defrecord AnthropicClient [api-url api-key model]
  LLMClient
  (chat [this messages] (chat this messages nil))
  (chat [_ messages tools]
    (let [{:keys [system messages]} (->anthropic-messages messages)
          payload (cond-> {:model      model
                           :max_tokens 4096
                           :messages   messages}
                    (seq system) (assoc :system system)
                    (seq tools)  (assoc :tools (->anthropic-tools tools)))
          resp (hato/post (str api-url "/messages")
                          {:headers {"x-api-key"         api-key
                                     "anthropic-version" "2023-06-01"
                                     "Content-Type"      "application/json"}
                           :body    (json/generate-string payload)
                           :as      :string})
          body (json/parse-string (:body resp) true)]
      (anthropic->reply body))))

;; ---------------------------------------------------------------------------

(defn make-client
  "Build an LLM client, selecting the provider from the api-url."
  [api-url api-key model]
  (if (str/includes? (str api-url) "anthropic")
    (->AnthropicClient api-url api-key model)
    (->OpenAIClient api-url api-key model)))
