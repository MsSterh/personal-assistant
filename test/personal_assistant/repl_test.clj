(ns personal-assistant.repl-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [personal-assistant.fake-client :refer [->FakeClient script-client]]
            [personal-assistant.memory :as memory]
            [personal-assistant.repl :as repl])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:dynamic *cwd* nil)

(defn- temp-dir ^File []
  (.toFile (Files/createTempDirectory "repl-test" (make-array FileAttribute 0))))

(use-fixtures :each
  (fn [t]
    (let [dir (.getCanonicalFile (temp-dir))
          prev (System/getProperty "user.dir")]
      (System/setProperty "user.dir" (.getPath dir))
      (try
        (binding [*cwd* dir] (t))
        (finally
          (System/setProperty "user.dir" prev))))))

(deftest single-turn-loop
  (testing "run-loop prints the LLM reply to stdout"
    (let [client     (->FakeClient "Fake reply here")
          system-msg {:role "system" :content "You are an assistant."}
          output     (with-out-str
                       (with-in-str "hello\n"
                         (repl/run-loop client system-msg)))]
      (is (str/includes? output "Fake reply here")))))

(deftest tool-call-cycle
  (testing "a tool-call turn is dispatched, then the final text reply prints"
    (spit (io/file *cwd* "note.txt") "secret contents")
    (let [tool-turn {:role "assistant"
                     :content nil
                     :tool_calls [{:id "c1"
                                   :type "function"
                                   :function {:name "read_file"
                                              :arguments (json/generate-string {:path "note.txt"})}}]}
          text-turn {:role "assistant" :content "The file says: secret contents"}
          client     (script-client [tool-turn text-turn])
          system-msg {:role "system" :content "You are an assistant."}
          output     (with-out-str
                       (with-in-str "read note.txt\n"
                         (repl/run-loop client system-msg)))]
      (is (str/includes? output "The file says: secret contents")))))

(deftest memory-marker-on-final-reply
  (testing "memory-marker extraction still runs on the final text reply"
    (let [reply      {:role "assistant"
                      :content "Saved that. <!-- remember: user likes Clojure -->"}
          client     (script-client [reply])
          system-msg {:role "system" :content "You are an assistant."}
          appended   (atom nil)
          output     (with-out-str
                       (with-redefs [memory/append-facts-to-daily-note
                                     (fn [facts & _] (reset! appended (vec facts)))]
                         (with-in-str "remember I like Clojure\n"
                           (repl/run-loop client system-msg))))]
      ;; Marker is stripped from the displayed text.
      (is (str/includes? output "Saved that."))
      (is (not (str/includes? output "remember:")))
      ;; The extracted fact was passed through to the memory writer.
      (is (= ["user likes Clojure"] @appended)))))
