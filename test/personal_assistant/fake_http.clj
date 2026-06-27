(ns personal-assistant.fake-http
  "Test double for `personal-assistant.http/HttpClient`. Records each request and
  returns a scripted response, so web tools can be tested without the network."
  (:require [personal-assistant.http :refer [HttpClient]]))

(defrecord FakeHttpClient [responses calls]
  HttpClient
  (http-get [_ url opts]
    (swap! calls conj {:url url :opts opts})
    (if (fn? responses)
      (responses url opts)
      responses)))

(defn fake
  "Build a FakeHttpClient. `responses` is either a single response map returned
  for every call, or a fn of `[url opts]` -> response map. Requests are recorded
  in the `:calls` atom."
  [responses]
  (->FakeHttpClient responses (atom [])))
