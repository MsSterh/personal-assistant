(ns personal-assistant.repl-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [personal-assistant.fake-client :refer [->FakeClient]]
            [personal-assistant.repl :as repl]))

(deftest single-turn-loop
  (testing "run-loop prints the LLM reply to stdout"
    (let [client     (->FakeClient "Fake reply here")
          system-msg {:role "system" :content "You are an assistant."}
          output     (with-out-str
                       (with-in-str "hello\n"
                         (repl/run-loop client system-msg)))]
      (is (str/includes? output "Fake reply here")))))
