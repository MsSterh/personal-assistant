(ns personal-assistant.fake-client
  (:require [personal-assistant.llm :refer [LLMClient]]))

(defrecord FakeClient [reply]
  LLMClient
  (chat [_ _messages]
    {:role "assistant" :content reply}))
