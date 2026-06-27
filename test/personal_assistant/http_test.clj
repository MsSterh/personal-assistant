(ns personal-assistant.http-test
  (:require [clojure.test :refer [deftest is testing]]
            [hato.client :as hato]
            [personal-assistant.fake-http :as fake-http]
            [personal-assistant.http :as http]))

(deftest fake-client-records-and-returns
  (testing "a fake HttpClient records the request and returns a scripted response"
    (let [client (fake-http/fake {:status 200 :body "hi" :headers {}})
          resp   (http/http-get client "http://example.com" {:query-params {:q "x"}})]
      (is (= 200 (:status resp)))
      (is (= "hi" (:body resp)))
      (is (= "http://example.com" (-> @(:calls client) first :url)))
      (is (= {:q "x"} (-> @(:calls client) first :opts :query-params))))))

(deftest fake-client-fn-responses
  (testing "responses may be a fn of url+opts"
    (let [client (fake-http/fake (fn [url _opts] {:status 200 :body url :headers {}}))]
      (is (= "http://a" (:body (http/http-get client "http://a" {}))))
      (is (= "http://b" (:body (http/http-get client "http://b" {})))))))

(deftest hato-client-maps-response
  (testing "HatoHttpClient returns {:status :body :headers} from hato/get"
    (with-redefs [hato/get (fn [_url _opts]
                             {:status 200 :body "page" :headers {"content-type" "text/html"}})]
      (let [resp (http/http-get (http/make-client) "http://example.com" {})]
        (is (= 200 (:status resp)))
        (is (= "page" (:body resp)))
        (is (= "text/html" (get (:headers resp) "content-type")))))))

(deftest hato-client-passes-throw-exceptions-false
  (testing "requests opt out of hato throwing, so non-2xx returns a status map"
    (let [captured (atom nil)]
      (with-redefs [hato/get (fn [_url opts]
                               (reset! captured opts)
                               {:status 404 :body "nope" :headers {}})]
        (let [resp (http/http-get (http/make-client) "http://example.com" {})]
          (is (false? (:throw-exceptions @captured)))
          (is (= 404 (:status resp)))
          (is (= "nope" (:body resp))))))))
