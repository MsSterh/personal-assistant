(ns personal-assistant.llm-test
  (:require [clojure.test :refer [deftest is testing]]
            [personal-assistant.fake-client :refer [->FakeClient]]
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
