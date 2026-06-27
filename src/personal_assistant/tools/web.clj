(ns personal-assistant.tools.web
  "Web tools exposed to the LLM: a DuckDuckGo HTML search and a URL reader that
  returns readable text (default) or raw HTML. Both take an injected
  `HttpClient`; the registry passes `*http-client*` so tests can rebind a fake.
  Every fn returns a plain, serializable map — failures are `{:error ..}`."
  (:require [clojure.string :as str]
            [personal-assistant.http :as http])
  (:import [java.net URLDecoder]
           [org.jsoup Jsoup]))

(def ^:dynamic *http-client*
  "HttpClient used by the registry-facing web tools. Rebind in tests to inject
  a fake without hitting the network."
  (http/make-client))

(def ^:private ddg-url "https://html.duckduckgo.com/html/")
(def ^:private max-results 8)
(def ^:private max-content-chars 20000)

(defn- decode-ddg-href
  "DuckDuckGo result links are redirect URLs shaped like
  `//duckduckgo.com/l/?uddg=<encoded-target>&rut=..`. Return the real target,
  or the original href when it isn't a redirect link."
  [href]
  (if-let [m (re-find #"[?&]uddg=([^&]+)" (or href ""))]
    (URLDecoder/decode (second m) "UTF-8")
    href))

(defn- parse-results [html]
  (->> (.select (Jsoup/parse html) "div.result")
       (keep (fn [el]
               (when-let [link (.selectFirst el "a.result__a")]
                 (let [snippet (.selectFirst el "a.result__snippet")]
                   {:title   (.text link)
                    :url     (decode-ddg-href (.attr link "href"))
                    :snippet (if snippet (.text snippet) "")}))))
       (take max-results)
       vec))

(defn search
  "Run a DuckDuckGo HTML search for `query`. Returns
  `{:query .. :results [{:title :url :snippet} ..]}` or `{:error ..}`."
  [client {:keys [query]}]
  (if (str/blank? query)
    {:error "query must not be blank"}
    (try
      (let [{:keys [status body]} (http/http-get client ddg-url {:query-params {:q query}})]
        (if (= 200 status)
          {:query query :results (parse-results body)}
          {:error (str "Search request failed with status " status)}))
      (catch Exception e
        {:error (str "Search failed: " (.getMessage e))}))))

(defn- header-value
  "Case-insensitive header lookup (hato lower-cases keys, but be defensive)."
  [headers k]
  (or (get headers k)
      (get headers (str/lower-case k))
      (get headers (str/capitalize k))))

(defn- html? [content-type]
  (let [ct (str/lower-case (or content-type ""))]
    (or (str/blank? ct)
        (str/includes? ct "html")
        (str/includes? ct "text"))))

(defn- extract-text [html]
  (let [doc (Jsoup/parse html)]
    (.remove (.select doc "script, style, noscript"))
    (let [body (.body doc)]
      (str/trim (.text (or body doc))))))

(defn- cap [s]
  (if (> (count s) max-content-chars)
    (subs s 0 max-content-chars)
    s))

(defn read-url
  "Fetch `url` and return its content. `format` is `\"text\"` (default,
  readable text via jsoup) or `\"html\"` (the unprocessed body). Returns the
  uniform shape `{:url :format :content}` or `{:error ..}`."
  [client {:keys [url format]}]
  (let [fmt (if (= "html" format) "html" "text")]
    (if (str/blank? url)
      {:error "url must not be blank"}
      (try
        (let [{:keys [status body headers]} (http/http-get client url {})
              content-type (header-value headers "content-type")]
          (cond
            (not= 200 status)
            {:error (str "Fetch failed with status " status)}

            (not (html? content-type))
            {:error (str "Unsupported content type: " content-type)}

            :else
            {:url     url
             :format  fmt
             :content (cap (if (= "html" fmt) body (extract-text body)))}))
        (catch Exception e
          {:error (str "Fetch failed: " (.getMessage e))})))))
