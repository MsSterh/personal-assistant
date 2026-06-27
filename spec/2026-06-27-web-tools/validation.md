# Phase 4 — Web Tools: Validation

## Automated Tests

- **HTTP protocol** (`http_test.clj`): a fake `HttpClient` returns scripted
  responses and records the requested URL; confirms the protocol contract.
- **web_search** (`tools/web_test.clj`):
  - Parses a captured DuckDuckGo HTML fixture into
    `{:results [{:title :url :snippet} ...]}` with real target URLs (redirect
    links decoded).
  - Returns `{:error ...}` on a non-200 fake response.
- **web_read** (`tools/web_test.clj`):
  - `format` `"text"` extracts plain text from an HTML fixture; `script`/
    `style`/`noscript` content is absent and whitespace is collapsed. Result is
    `{:url :format "text" :content ...}`.
  - `format` `"html"` returns the unprocessed HTML body unchanged, as
    `{:url :format "html" :content ...}`.
  - Returns `{:error ...}` on non-200 or non-HTML content.
- **Registry dispatch** (`registry_test.clj`): `dispatch` routes a `web_search`
  and a `web_read` tool_call (with a fake `HttpClient`) and returns a
  well-formed `{:role "tool" :tool_call_id .. :content <json>}` message.
- **Anthropic schema translation** (`llm_test.clj`): both web tool schemas pass
  through `->anthropic-tools` into the `{:name :description :input_schema}`
  shape, confirming the generic conversion covers them.
- No test performs real network I/O.

## Manual Checks

- Run the console REPL with a real LLM + a live network.
- Pose a question requiring fresh info; confirm the assistant calls `web_search`
  and the results print in the tool-call trace.
- Ask a follow-up that makes the assistant `web_read` one of the result URLs;
  confirm extracted text comes back and informs the reply.
- Confirm a search/fetch failure (e.g. a bad URL) yields a graceful
  `{:error ...}` the assistant can report, not a crash.

## Definition of Done

- [ ] `web_search` and `web_read` are registered and invoked by the LLM via the
      existing dispatch layer, with no change to `dispatch`'s contract.
- [ ] HTTP goes through the new `HttpClient` protocol; tools take an injected
      client.
- [ ] jsoup added to `deps.edn` and used for both result parsing and text
      extraction.
- [ ] All new and existing tests pass (`clojure -M:test` or project equivalent).
- [ ] clj-kondo and cljfmt report no issues on the new namespaces.
- [ ] Manual REPL session demonstrates a search → read → answer flow.
- [ ] Roadmap Phase 4 acceptance ("web search, web page reader, reuse registry,
      fake-HTTP tests") is satisfied.
