(ns personal-assistant.fake-client
  (:require [personal-assistant.llm :refer [LLMClient]]))

;; A FakeClient can be constructed two ways:
;;   (->FakeClient "some text")        -> always replies with that text
;;   (script-client [msg1 msg2 ...])   -> returns each scripted assistant
;;                                        message in order on successive calls
;; Scripted messages are plain maps, e.g. a tool-call turn followed by a final
;; text reply, letting tests drive a full dispatch cycle.

(defrecord FakeClient [reply]
  LLMClient
  (chat [this messages] (.chat this messages nil))
  (chat [_ _messages _tools]
    {:role "assistant" :content reply}))

(defrecord ScriptedClient [replies calls]
  LLMClient
  (chat [this messages] (.chat this messages nil))
  (chat [_ _messages _tools]
    (let [i (dec (swap! calls inc))]
      (nth replies (min i (dec (count replies)))))))

(defn script-client
  "FakeClient-like client that returns each message in `replies` in turn."
  [replies]
  (->ScriptedClient (vec replies) (atom 0)))
