(ns personal-assistant.http
  "HTTP access behind a protocol so the web tools can be tested with a fake
  client instead of hitting the network — mirrors the `LLMClient` protocol in
  `personal-assistant.llm`."
  (:require [hato.client :as hato]))

(defprotocol HttpClient
  (http-get [client url opts]
    "GET `url`. Returns `{:status :body :headers}`. Implementations should
     surface non-2xx responses as a normal result (with `:status`) rather than
     throwing; transport failures may still throw."))

(def default-user-agent
  "personal-assistant/0.1 (+https://github.com/MsSterh/personal-assistant)")

(def ^:private base-opts
  {:headers          {"User-Agent" default-user-agent}
   :as               :string
   :throw-exceptions false
   :redirect-policy  :normal})

(defrecord HatoHttpClient [opts]
  HttpClient
  (http-get [_ url request-opts]
    ;; :throw-exceptions false makes hato return non-2xx as a normal response
    ;; map, so callers branch on :status. Transport failures (DNS, refused,
    ;; timeout) still throw and are handled by the calling tool.
    (let [resp (hato/get url (merge base-opts opts request-opts))]
      {:status  (:status resp)
       :body    (:body resp)
       :headers (:headers resp)})))

(defn make-client
  "Build the default hato-backed HttpClient. `opts` are merged into every
  request (after the built-in defaults, before per-call opts)."
  ([] (make-client {}))
  ([opts] (->HatoHttpClient opts)))
