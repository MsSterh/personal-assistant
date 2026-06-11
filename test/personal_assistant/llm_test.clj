(ns personal-assistant.llm-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is testing]]
            [hato.client :as hato]
            [personal-assistant.fake-client :refer [->FakeClient script-client]]
            [personal-assistant.llm :as llm]))

(deftest fake-client-round-trip
  (testing "FakeClient reply appears in conversation history"
    (let [client  (->FakeClient "Hello from fake!")
          history (atom [{:role "system" :content "You are an assistant."}
                         {:role "user" :content "hi"}])
          reply   (llm/chat client @history)]
      (swap! history conj reply)
      (is (= "Hello from fake!" (-> @history last :content)))
      (is (= "assistant" (-> @history last :role))))))

(deftest openai-client-includes-tools
  (testing "request body includes tool definitions when tools are passed"
    (let [captured (atom nil)]
      (with-redefs [hato.client/post
                    (fn [_url opts]
                      (reset! captured (json/parse-string (:body opts) true))
                      {:body (json/generate-string
                              {:choices [{:message {:role "assistant" :content "ok"}}]})})]
        (let [client (llm/->OpenAIClient "http://x" "key" "model")
              tools  [{:type "function" :function {:name "read_file"}}]]
          (llm/chat client [{:role "user" :content "hi"}] tools)
          (is (= tools (:tools @captured))))))))

(deftest openai-client-surfaces-tool-calls
  (testing "tool_calls on the response are surfaced on the returned message"
    (with-redefs [hato.client/post
                  (fn [_url _opts]
                    {:body (json/generate-string
                            {:choices [{:message {:role "assistant"
                                                  :content nil
                                                  :tool_calls [{:id "c1"
                                                                :type "function"
                                                                :function {:name "read_file"
                                                                           :arguments "{\"path\":\"x\"}"}}]}}]})})]
      (let [client (llm/->OpenAIClient "http://x" "key" "model")
            reply  (llm/chat client [{:role "user" :content "read x"}] [])]
        (is (= "c1" (-> reply :tool_calls first :id)))
        (is (= "read_file" (-> reply :tool_calls first :function :name)))))))

(deftest anthropic-client-translates-request
  (testing "system is hoisted, tools converted, and the messages API is hit"
    (let [captured (atom nil) url (atom nil)]
      (with-redefs [hato.client/post
                    (fn [u opts]
                      (reset! url u)
                      (reset! captured (json/parse-string (:body opts) true))
                      {:body (json/generate-string
                              {:content [{:type "text" :text "ok"}]})})]
        (let [client (llm/->AnthropicClient "http://x" "key" "model")
              tools  [{:type "function"
                       :function {:name "read_file"
                                  :description "Read a file."
                                  :parameters {:type "object"}}}]]
          (llm/chat client [{:role "system" :content "be terse"}
                            {:role "user" :content "hi"}]
                    tools)
          (is (= "http://x/messages" @url))
          (is (= "be terse" (:system @captured)))
          (is (= 4096 (:max_tokens @captured)))
          ;; system message is removed from the messages array
          (is (= [{:role "user" :content "hi"}] (:messages @captured)))
          ;; OpenAI function schema -> Anthropic tool shape
          (is (= {:name "read_file" :description "Read a file."
                  :input_schema {:type "object"}}
                 (first (:tools @captured)))))))))

(deftest anthropic-client-translates-tool-roundtrip
  (testing "assistant tool_calls and tool results convert to content blocks"
    (let [captured (atom nil)]
      (with-redefs [hato.client/post
                    (fn [_u opts]
                      (reset! captured (json/parse-string (:body opts) true))
                      {:body (json/generate-string {:content [{:type "text" :text "done"}]})})]
        (let [client (llm/->AnthropicClient "http://x" "key" "model")]
          (llm/chat client
                    [{:role "user" :content "read x"}
                     {:role "assistant" :content ""
                      :tool_calls [{:id "c1" :type "function"
                                    :function {:name "read_file"
                                               :arguments "{\"path\":\"x\"}"}}]}
                     {:role "tool" :tool_call_id "c1" :content "file body"}]
                    nil)
          (let [msgs (:messages @captured)]
            ;; assistant turn carries a tool_use block with parsed input
            (is (= {:type "tool_use" :id "c1" :name "read_file" :input {:path "x"}}
                   (-> msgs second :content first)))
            ;; tool result becomes a user turn with a tool_result block
            (is (= {:role "user"
                    :content [{:type "tool_result" :tool_use_id "c1" :content "file body"}]}
                   (nth msgs 2)))))))))

(deftest anthropic-client-surfaces-tool-calls
  (testing "tool_use blocks in the response become OpenAI-style tool_calls"
    (with-redefs [hato.client/post
                  (fn [_u _opts]
                    {:body (json/generate-string
                            {:content [{:type "text" :text "calling"}
                                       {:type "tool_use" :id "c9" :name "read_file"
                                        :input {:path "x"}}]})})]
      (let [client (llm/->AnthropicClient "http://x" "key" "model")
            reply  (llm/chat client [{:role "user" :content "read x"}] [])]
        (is (= "calling" (:content reply)))
        (is (= "c9" (-> reply :tool_calls first :id)))
        (is (= "read_file" (-> reply :tool_calls first :function :name)))
        ;; arguments come back as a JSON string, as the dispatch path expects
        (is (= "{\"path\":\"x\"}" (-> reply :tool_calls first :function :arguments)))))))

(deftest scripted-client-returns-in-order
  (testing "scripted client returns each reply in turn"
    (let [client (script-client [{:role "assistant" :content "first"}
                                 {:role "assistant" :content "second"}])]
      (is (= "first" (:content (llm/chat client []))))
      (is (= "second" (:content (llm/chat client [])))))))
