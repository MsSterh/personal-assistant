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

(deftest hato-client-includes-tools
  (testing "request body includes tool definitions when tools are passed"
    (let [captured (atom nil)]
      (with-redefs [hato.client/post
                    (fn [_url opts]
                      (reset! captured (json/parse-string (:body opts) true))
                      {:body (json/generate-string
                              {:choices [{:message {:role "assistant" :content "ok"}}]})})]
        (let [client (llm/->HatoClient "http://x" "key" "model")
              tools  [{:type "function" :function {:name "read_file"}}]]
          (llm/chat client [{:role "user" :content "hi"}] tools)
          (is (= tools (:tools @captured))))))))

(deftest hato-client-surfaces-tool-calls
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
      (let [client (llm/->HatoClient "http://x" "key" "model")
            reply  (llm/chat client [{:role "user" :content "read x"}] [])]
        (is (= "c1" (-> reply :tool_calls first :id)))
        (is (= "read_file" (-> reply :tool_calls first :function :name)))))))

(deftest scripted-client-returns-in-order
  (testing "scripted client returns each reply in turn"
    (let [client (script-client [{:role "assistant" :content "first"}
                                 {:role "assistant" :content "second"}])]
      (is (= "first" (:content (llm/chat client []))))
      (is (= "second" (:content (llm/chat client [])))))))
