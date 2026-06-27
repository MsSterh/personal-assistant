(ns personal-assistant.tools.web-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [personal-assistant.fake-http :as fake-http]
            [personal-assistant.tools.web :as web]))

(def ddg-html
  "<html><body>
     <div class=\"result results_links\">
       <a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage&amp;rut=abc\">Example Title</a>
       <a class=\"result__snippet\">An example snippet.</a>
     </div>
     <div class=\"result results_links\">
       <a class=\"result__a\" href=\"//duckduckgo.com/l/?uddg=https%3A%2F%2Fclojure.org%2F\">Clojure</a>
       <a class=\"result__snippet\">The Clojure language.</a>
     </div>
   </body></html>")

(def page-html
  "<html><head><title>Page Title</title><style>.x{color:red}</style></head>
   <body><script>var secret = 1;</script><h1>Heading</h1><p>Hello world.</p></body></html>")

(def rich-html
  "<html><head><title>Doc</title></head>
   <body>
     <h1>Title   With   Spaces</h1>
     <p>First   paragraph
        spanning lines.</p>
     <p>Tom &amp; Jerry &lt;tags&gt; x&nbsp;y.</p>
     <ul><li>One</li><li>Two</li></ul>
     <!-- a comment that should vanish -->
     <p>Link: <a href=\"http://x\">click <b>here</b></a> now.</p>
     <noscript>enable scripts</noscript>
   </body></html>")

(deftest search-parses-results
  (testing "search parses DuckDuckGo HTML into result maps with decoded URLs"
    (let [client (fake-http/fake {:status 200 :body ddg-html :headers {}})
          result (web/search client {:query "clojure"})]
      (is (= "clojure" (:query result)))
      (is (= 2 (count (:results result))))
      (is (= {:title "Example Title"
              :url "https://example.com/page"
              :snippet "An example snippet."}
             (first (:results result))))
      (is (= "https://clojure.org/" (-> result :results second :url)))
      ;; the query was sent through as a query-param
      (is (= {:q "clojure"} (-> @(:calls client) first :opts :query-params))))))

(deftest search-blank-query
  (testing "a blank query is rejected before any request"
    (let [client (fake-http/fake {:status 200 :body ddg-html :headers {}})]
      (is (= {:error "query must not be blank"} (web/search client {:query "  "})))
      (is (empty? @(:calls client))))))

(deftest search-non-200
  (testing "a non-200 search response yields a structured error"
    (let [client (fake-http/fake {:status 503 :body "" :headers {}})
          result (web/search client {:query "x"})]
      (is (str/includes? (:error result) "503")))))

(deftest read-url-text
  (testing "format text extracts readable text, dropping script/style"
    (let [client (fake-http/fake {:status 200 :body page-html
                                  :headers {"content-type" "text/html"}})
          result (web/read-url client {:url "http://example.com" :format "text"})]
      (is (= "text" (:format result)))
      (is (= "http://example.com" (:url result)))
      (is (str/includes? (:content result) "Heading"))
      (is (str/includes? (:content result) "Hello world."))
      (is (not (str/includes? (:content result) "secret")))
      (is (not (str/includes? (:content result) "color:red"))))))

(deftest read-url-text-default
  (testing "format defaults to text when omitted"
    (let [client (fake-http/fake {:status 200 :body page-html
                                  :headers {"content-type" "text/html"}})
          result (web/read-url client {:url "http://example.com"})]
      (is (= "text" (:format result)))
      (is (not (str/includes? (:content result) "<h1>"))))))

(deftest read-url-html
  (testing "format html returns the unprocessed body"
    (let [client (fake-http/fake {:status 200 :body page-html
                                  :headers {"content-type" "text/html"}})
          result (web/read-url client {:url "http://example.com" :format "html"})]
      (is (= "html" (:format result)))
      (is (= page-html (:content result)))
      (is (str/includes? (:content result) "<script>")))))

(deftest read-url-text-collapses-whitespace
  (testing "runs of spaces and newlines collapse to single spaces"
    (let [client  (fake-http/fake {:status 200 :body rich-html
                                   :headers {"content-type" "text/html"}})
          content (:content (web/read-url client {:url "http://x"}))]
      (is (str/includes? content "Title With Spaces"))
      (is (str/includes? content "First paragraph spanning lines."))
      (is (not (str/includes? content "  ")))
      (is (not (str/includes? content "\n"))))))

(deftest read-url-text-decodes-entities
  (testing "HTML entities are decoded to their characters"
    (let [client  (fake-http/fake {:status 200 :body rich-html
                                   :headers {"content-type" "text/html"}})
          content (:content (web/read-url client {:url "http://x"}))]
      (is (str/includes? content "Tom & Jerry <tags> x y.")))))

(deftest read-url-text-separates-blocks
  (testing "block elements and list items are separated, not glued together"
    (let [client  (fake-http/fake {:status 200 :body rich-html
                                   :headers {"content-type" "text/html"}})
          content (:content (web/read-url client {:url "http://x"}))]
      (is (str/includes? content "One Two")))))

(deftest read-url-text-flattens-inline-and-drops-comments
  (testing "nested inline tags become continuous text; comments are removed"
    (let [client  (fake-http/fake {:status 200 :body rich-html
                                   :headers {"content-type" "text/html"}})
          content (:content (web/read-url client {:url "http://x"}))]
      (is (str/includes? content "Link: click here now."))
      (is (not (str/includes? content "comment")))
      (is (not (str/includes? content "enable scripts"))))))

(deftest read-url-non-200
  (testing "a non-200 fetch yields a structured error"
    (let [client (fake-http/fake {:status 404 :body "" :headers {}})
          result (web/read-url client {:url "http://example.com"})]
      (is (str/includes? (:error result) "404")))))

(deftest read-url-non-html
  (testing "a non-HTML content type is rejected"
    (let [client (fake-http/fake {:status 200 :body "{}"
                                  :headers {"content-type" "application/json"}})
          result (web/read-url client {:url "http://example.com/data.json"})]
      (is (str/includes? (:error result) "Unsupported content type")))))

(deftest read-url-blank
  (testing "a blank url is rejected before any request"
    (let [client (fake-http/fake {:status 200 :body "" :headers {}})]
      (is (= {:error "url must not be blank"} (web/read-url client {:url ""})))
      (is (empty? @(:calls client))))))
