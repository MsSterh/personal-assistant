# Phase 4 — Web Tools: Plan

## 1. Dependencies

- [ ] Add `org.jsoup/jsoup` to `deps.edn`.
- [ ] Confirm hato is already available (it is, via the LLM client).

## 2. HTTP client protocol

- [ ] Create `src/personal_assistant/http.clj`.
- [ ] `defprotocol HttpClient` with `http-get` (url, opts) — returns a map
      `{:status :body :headers}` or throws on transport failure.
- [ ] `HatoHttpClient` record implementing it via `hato.client/get` with a
      sensible default User-Agent and redirect/timeout handling.
- [ ] `make-client` constructor.

## 3. Web search tool

- [ ] Create `src/personal_assistant/tools/web.clj`.
- [ ] `search` — takes `{:keys [query]}` plus an injected `HttpClient`; GETs the
      DuckDuckGo HTML endpoint with the query.
- [ ] Parse results with jsoup: extract title, URL, and snippet for each result
      row; decode DuckDuckGo's redirect links to the real target URL.
- [ ] Return `{:results [{:title :url :snippet} ...]}`, capped at a small N;
      `{:error ...}` on non-200 or parse failure.

## 4. Web page reader tool

- [ ] `read-url` in `tools/web.clj` — takes `{:keys [url format]}` plus the
      `HttpClient`; GETs the URL. `format` is a string, `"text"` (default) or
      `"html"`.
- [ ] `"text"`: parse with jsoup; remove `script`/`style`/`noscript`; extract
      readable text and collapse whitespace.
- [ ] `"html"`: skip extraction; use the unprocessed response body.
- [ ] Return the uniform shape `{:url :format :content}` (content
      length-capped); `{:error ...}` on non-200, non-HTML content type, or fetch
      failure.

## 5. Registry integration

- [ ] Add `web_search` and `web_read` entries to the `tools` map in
      `src/personal_assistant/tools/registry.clj` with OpenAI-format schemas.
- [ ] Ensure their `:fn`s receive an `HttpClient`. Either close over a
      default client when building the registry, or thread the client through
      `dispatch`/`tool-defs` — pick the approach that keeps `dispatch`'s
      external contract unchanged.
- [ ] Verify the new schemas appear in `tool-defs` automatically.
- [ ] Confirm the schemas flow through `llm/->anthropic-tools` to the Anthropic
      `{:name :description :input_schema}` shape — the conversion is generic, so
      no per-tool code is needed, but add a test asserting both web tools
      translate correctly alongside the OpenAI passthrough.

## 6. REPL / wiring

- [ ] Construct a `HatoHttpClient` at startup (alongside the LLM client) and
      make it available to the web tools.
- [ ] No change to the tool-call loop itself — it already dispatches whatever
      tools the registry exposes.

## 7. Tests

- [ ] `test/personal_assistant/http_test.clj` — protocol shape; a fake client
      records calls and returns scripted responses.
- [ ] `test/personal_assistant/tools/web_test.clj`:
  - [ ] `search` parses a captured DuckDuckGo HTML fixture into result maps.
  - [ ] `search` returns `{:error ...}` on a non-200 fake response.
  - [ ] `read-url` with `format` `"text"` extracts text from an HTML fixture,
        dropping script/style; returns `{:url :format "text" :content ...}`.
  - [ ] `read-url` with `format` `"html"` returns the unprocessed HTML body as
        `{:url :format "html" :content ...}`.
  - [ ] `read-url` errors on non-200 / non-HTML responses.
- [ ] Extend `test/personal_assistant/tools/registry_test.clj`: dispatch a
      `web_search` and a `web_read` tool_call through `dispatch` with a fake
      `HttpClient`, asserting the returned `{:role "tool" ...}` message.

## 8. Lint, format, manual check

- [ ] clj-kondo clean across new namespaces.
- [ ] cljfmt clean.
- [ ] Manual REPL session: ask a question that triggers a `web_search`, then a
      follow-up that triggers `web_read` on one of the results, and confirm the
      assistant uses the fetched content.
