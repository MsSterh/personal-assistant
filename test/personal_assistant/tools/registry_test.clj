(ns personal-assistant.tools.registry-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [personal-assistant.fake-http :as fake-http]
            [personal-assistant.tools.registry :as registry]
            [personal-assistant.tools.web :as web])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:dynamic *cwd* nil)

(defn- temp-dir ^File []
  (.toFile (Files/createTempDirectory "registry-test" (make-array FileAttribute 0))))

(use-fixtures :each
  (fn [t]
    (let [dir (.getCanonicalFile (temp-dir))
          prev (System/getProperty "user.dir")]
      (System/setProperty "user.dir" (.getPath dir))
      (try
        (binding [*cwd* dir] (t))
        (finally
          (System/setProperty "user.dir" prev))))))

(defn- tool-call [id name args]
  {:id id :type "function"
   :function {:name name :arguments (json/generate-string args)}})

(deftest dispatch-returns-tool-message
  (testing "dispatch routes to the correct fn and returns a tool-role message"
    (spit (io/file *cwd* "hello.txt") "hi there")
    (let [msg (registry/dispatch (tool-call "call_1" "read_file" {:path "hello.txt"}))]
      (is (= "tool" (:role msg)))
      (is (= "call_1" (:tool_call_id msg)))
      (is (= "hi there" (:content (json/parse-string (:content msg) true)))))))

(deftest dispatch-write-tool
  (testing "dispatch routes write_file and the args are parsed from JSON"
    (let [msg (registry/dispatch (tool-call "c2" "write_file" {:path "w.txt" :content "data"}))
          result (json/parse-string (:content msg) true)]
      (is (nil? (:error result)))
      (is (= "data" (slurp (io/file *cwd* "w.txt")))))))

(deftest dispatch-unknown-tool
  (testing "unknown tool name yields a structured error, not an exception"
    (let [msg (registry/dispatch (tool-call "c3" "nope" {}))
          result (json/parse-string (:content msg) true)]
      (is (= "tool" (:role msg)))
      (is (re-find #"Unknown tool" (:error result))))))

(deftest dispatch-web-search
  (testing "dispatch routes web_search through the bound http client"
    (let [html "<div class=\"result\"><a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fclojure.org\">Clojure</a><a class=\"result__snippet\">Lang.</a></div>"
          client (fake-http/fake {:status 200 :body html :headers {}})]
      (binding [web/*http-client* client]
        (let [msg    (registry/dispatch (tool-call "w1" "web_search" {:query "clojure"}))
              result (json/parse-string (:content msg) true)]
          (is (= "tool" (:role msg)))
          (is (= "w1" (:tool_call_id msg)))
          (is (= "https://clojure.org" (-> result :results first :url))))))))

(deftest dispatch-web-read
  (testing "dispatch routes web_read through the bound http client"
    (let [client (fake-http/fake {:status 200 :body "<html><body><p>Hi there</p></body></html>"
                                  :headers {"content-type" "text/html"}})]
      (binding [web/*http-client* client]
        (let [msg    (registry/dispatch (tool-call "w2" "web_read" {:url "http://x" :format "text"}))
              result (json/parse-string (:content msg) true)]
          (is (= "text" (:format result)))
          (is (= "Hi there" (:content result))))))))

(deftest tool-defs-shape
  (testing "tool-defs is a vector of OpenAI-format function schemas"
    (is (vector? registry/tool-defs))
    (is (= 9 (count registry/tool-defs)))
    (is (every? #(= "function" (:type %)) registry/tool-defs))
    (is (= #{"read_file" "write_file" "append_file" "edit_file" "delete_file"
             "move_file" "list_dir" "web_search" "web_read"}
           (set (map #(get-in % [:function :name]) registry/tool-defs))))))
